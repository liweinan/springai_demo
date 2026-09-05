#!/usr/bin/env bash
# 对已开启 JDWP 的 backend 走一遍 jdb 断点：BookingService.listByStatus
# 不调用 DeepSeek。依赖：jdb、expect、curl；目标进程已监听 JDWP 且 /api/health/live 可达。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JDWP_HOST="${JDWP_HOST:-127.0.0.1}"
JDWP_PORT="${JDWP_PORT:-5005}"
API_BASE="${API_BASE:-http://127.0.0.1:8080}"
SOURCEPATH="${SOURCEPATH:-$ROOT/backend/src/main/java}"
WORK="$(mktemp -d "${TMPDIR:-/tmp}/jdb-debug-demo.XXXXXX")"
JDB_TRANSCRIPT="$WORK/jdb.transcript"
JDB_CURL_BODY="$WORK/bookings.json"
JDB_CURL_META="$WORK/curl.meta"
JDB_PID=""

cleanup() {
  if [[ -n "$JDB_PID" ]] && kill -0 "$JDB_PID" 2>/dev/null; then
    kill "$JDB_PID" 2>/dev/null || true
    wait "$JDB_PID" 2>/dev/null || true
  fi
  rm -rf "$WORK"
}
trap cleanup EXIT

die() {
  echo "jdb-demo FAIL: $*" >&2
  if [[ -f "$JDB_TRANSCRIPT" ]]; then
    echo "----- jdb transcript -----" >&2
    cat "$JDB_TRANSCRIPT" >&2
  fi
  if [[ -f "$JDB_CURL_META" ]]; then
    echo "----- curl meta -----" >&2
    cat "$JDB_CURL_META" >&2
  fi
  exit 1
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "缺少命令 $1"
}

wait_tcp() {
  local host="$1" port="$2" i
  for i in $(seq 1 40); do
    if command -v nc >/dev/null 2>&1 && nc -z "$host" "$port" >/dev/null 2>&1; then
      return 0
    fi
    if bash -c "echo >/dev/tcp/${host}/${port}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

wait_http() {
  local url="$1" i
  for i in $(seq 1 40); do
    if curl -fsS --max-time 2 "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  return 1
}

need_cmd jdb
need_cmd expect
need_cmd curl

echo "jdb-demo: 等待 ${API_BASE}/api/health/live"
wait_http "${API_BASE}/api/health/live" || die "${API_BASE}/api/health/live 不可达"

echo "jdb-demo: 等待 JDWP ${JDWP_HOST}:${JDWP_PORT}"
wait_tcp "$JDWP_HOST" "$JDWP_PORT" || die "JDWP ${JDWP_HOST}:${JDWP_PORT} 未监听（需 JDWP_ENABLED=true）"

export JDWP_HOST JDWP_PORT API_BASE SOURCEPATH JDB_TRANSCRIPT JDB_CURL_BODY JDB_CURL_META

expect <<'EOF' >"$JDB_TRANSCRIPT"
set timeout 25
log_user 1
set prompt_re {(>|\S+\[[0-9]+\])\s*$}

spawn jdb -attach $env(JDWP_HOST):$env(JDWP_PORT) -sourcepath $env(SOURCEPATH)
expect {
    -re $prompt_re {}
    -re {无法附加|Unable to attach|Connection refused|Connection reset} {
        puts stderr "jdb 无法附加，端口上可能已有其它调试器"
        exit 2
    }
    timeout { puts stderr "jdb attach 超时"; exit 2 }
}

send "stop in com.demo.booking.service.BookingService.listByStatus\r"
expect {
    -re {设置断点|Set breakpoint|Deferring breakpoint} {}
    -re {Unable to set|无法设置} { puts stderr "无法设置断点"; exit 3 }
    timeout { puts stderr "stop 超时"; exit 3 }
}
expect {
    -re $prompt_re {}
    timeout { puts stderr "stop 后无提示符"; exit 3 }
}

exec sh -c {
  trap '' HUP
  curl -sS --max-time 25 "$API_BASE/api/bookings?status=UNSUBSCRIBED" \
    -o "$JDB_CURL_BODY.tmp" \
    -w "CURL_HTTP=%{http_code}\n" \
    > "$JDB_CURL_META.tmp" 2>&1
  mv "$JDB_CURL_BODY.tmp" "$JDB_CURL_BODY"
  mv "$JDB_CURL_META.tmp" "$JDB_CURL_META"
} &

expect {
    -re {断点命中|Breakpoint hit} {}
    timeout { puts stderr "未命中 listByStatus 断点"; exit 3 }
}
expect {
    -re $prompt_re {}
    timeout { puts stderr "命中后无提示符"; exit 3 }
}

send "where\r"
expect {
    -re $prompt_re {}
    timeout { puts stderr "where 超时"; exit 3 }
}

send "list\r"
expect {
    -re $prompt_re {}
    timeout { puts stderr "list 超时"; exit 3 }
}

send "locals\r"
expect {
    -re $prompt_re {}
    timeout { puts stderr "locals 超时"; exit 3 }
}

send "print status\r"
expect {
    -re $prompt_re {}
    timeout { puts stderr "print status 超时"; exit 3 }
}

send "clear com.demo.booking.service.BookingService.listByStatus\r"
expect {
    -re $prompt_re {}
    timeout { puts stderr "clear 超时"; exit 3 }
}

send "cont\r"
set timeout 20
set waited 0
while {$waited < 20} {
    if {[file exists $env(JDB_CURL_META)]} {
        set meta_fd [open $env(JDB_CURL_META) r]
        set meta [read $meta_fd]
        close $meta_fd
        if {[string match {*CURL_HTTP=*} $meta]} {
            break
        }
    }
    after 250
    set waited [expr {$waited + 0.25}]
}
send "quit\r"
expect eof
EOF

expect_rc=$?
if [[ "$expect_rc" -ne 0 ]]; then
  die "expect 退出码 ${expect_rc}"
fi

if [[ ! -f "$JDB_CURL_META" ]]; then
  for _ in $(seq 1 20); do
    [[ -f "$JDB_CURL_META" ]] && break
    sleep 0.25
  done
fi

grep -Eq '断点命中|Breakpoint hit' "$JDB_TRANSCRIPT" || die "transcript 无断点命中"
grep -F 'BookingService.listByStatus' "$JDB_TRANSCRIPT" >/dev/null || die "stack 无 BookingService.listByStatus"
grep -F 'BookingController.listBookings' "$JDB_TRANSCRIPT" >/dev/null || die "stack 无 BookingController.listBookings"
grep -Eq 'status = "UNSUBSCRIBED"' "$JDB_TRANSCRIPT" || die "print status 不是 UNSUBSCRIBED"
grep -Eq 'listByStatus\(BookingStatus status\)|return bookingRepository.findByStatus' "$JDB_TRANSCRIPT" \
  || die "list 未显示 BookingService 源码（检查 SOURCEPATH）"

grep -q 'CURL_HTTP=200' "$JDB_CURL_META" || die "curl 未返回 HTTP 200"
grep -q 'UNSUBSCRIBED' "$JDB_CURL_BODY" || die "响应 JSON 无 UNSUBSCRIBED"

echo "jdb-demo PASS"
echo "  attach  ${JDWP_HOST}:${JDWP_PORT}"
echo "  break   BookingService.listByStatus"
echo "  stack   BookingController.listBookings -> BookingService.listByStatus"
echo "  locals  status=UNSUBSCRIBED"
echo "  http    GET /api/bookings?status=UNSUBSCRIBED -> 200"
