from __future__ import annotations

import time
from dataclasses import dataclass, field
from typing import Any, Mapping, Optional


CLOSED = "closed"
OPEN = "open"
HALF_OPEN = "half_open"


@dataclass
class CircuitState:
    key: str
    state: str = CLOSED
    consecutive_failures: int = 0
    half_open_in_flight: int = 0
    half_open_successes: int = 0
    opened_at: Optional[float] = None
    last_failure_at: Optional[float] = None
    last_success_at: Optional[float] = None
    last_error: Optional[str] = None


@dataclass(frozen=True)
class CircuitBreakerDecision:
    allowed: bool
    reason: Optional[str] = None
    state: Optional[str] = None


@dataclass(frozen=True)
class CircuitBreakerConfig:
    enabled: bool = True
    failure_threshold: int = 5
    recovery_seconds: float = 30.0
    half_open_max_requests: int = 2
    half_open_success_threshold: int = 2
    failure_status_threshold: int = 500

    @classmethod
    def from_mapping(cls, values: Mapping[str, Any]) -> "CircuitBreakerConfig":
        return cls(
            enabled=bool(values.get("enabled", True)),
            failure_threshold=int(_value(values, "failure-threshold", "failureThreshold", default=5)),
            recovery_seconds=float(_value(values, "recovery-seconds", "recoverySeconds", default=30)),
            half_open_max_requests=int(
                _value(values, "half-open-max-requests", "halfOpenMaxRequests", default=2)
            ),
            half_open_success_threshold=int(
                _value(
                    values,
                    "half-open-success-threshold",
                    "halfOpenSuccessThreshold",
                    default=2,
                )
            ),
            failure_status_threshold=int(
                _value(values, "failure-status-threshold", "failureStatusThreshold", default=500)
            ),
        )


class CircuitBreakerRegistry:
    def __init__(self, config: Mapping[str, Any]) -> None:
        self.config = CircuitBreakerConfig.from_mapping(config)
        self._states: dict[str, CircuitState] = {}

    def before_request(self, key: str) -> CircuitBreakerDecision:
        if not self.config.enabled:
            return CircuitBreakerDecision(True, state=CLOSED)
        state = self._state(key)
        now = time.time()
        if state.state == OPEN:
            opened_at = state.opened_at or now
            if now - opened_at >= self.config.recovery_seconds:
                state.state = HALF_OPEN
                state.half_open_in_flight = 0
                state.half_open_successes = 0
            else:
                return CircuitBreakerDecision(False, "服务熔断中，等待自动恢复", OPEN)
        if state.state == HALF_OPEN:
            if state.half_open_in_flight >= self.config.half_open_max_requests:
                return CircuitBreakerDecision(False, "服务熔断半开探测中", HALF_OPEN)
            state.half_open_in_flight += 1
        return CircuitBreakerDecision(True, state=state.state)

    def after_request(
        self,
        key: str,
        status_code: Optional[int] = None,
        error: Optional[str] = None,
    ) -> None:
        if not self.config.enabled:
            return
        state = self._state(key)
        failed = error is not None or (
            status_code is not None and status_code >= self.config.failure_status_threshold
        )
        if state.state == HALF_OPEN and state.half_open_in_flight > 0:
            state.half_open_in_flight -= 1
        if failed:
            self._record_failure(state, error or "HTTP %s" % status_code)
            return
        self._record_success(state)

    def reset(self, key: Optional[str] = None) -> None:
        if key:
            self._states.pop(key, None)
            return
        self._states.clear()

    def snapshot(self) -> dict[str, Any]:
        return {
            "enabled": self.config.enabled,
            "failureThreshold": self.config.failure_threshold,
            "recoverySeconds": self.config.recovery_seconds,
            "halfOpenMaxRequests": self.config.half_open_max_requests,
            "halfOpenSuccessThreshold": self.config.half_open_success_threshold,
            "states": [serialize_state(state) for state in self._states.values()],
        }

    def _state(self, key: str) -> CircuitState:
        state = self._states.get(key)
        if state is None:
            state = CircuitState(key=key)
            self._states[key] = state
        return state

    def _record_failure(self, state: CircuitState, error: str) -> None:
        now = time.time()
        state.last_failure_at = now
        state.last_error = error
        state.consecutive_failures += 1
        state.half_open_successes = 0
        if state.state == HALF_OPEN or state.consecutive_failures >= self.config.failure_threshold:
            state.state = OPEN
            state.opened_at = now
            state.half_open_in_flight = 0

    def _record_success(self, state: CircuitState) -> None:
        now = time.time()
        state.last_success_at = now
        state.last_error = None
        if state.state == HALF_OPEN:
            state.half_open_successes += 1
            if state.half_open_successes >= self.config.half_open_success_threshold:
                state.state = CLOSED
                state.consecutive_failures = 0
                state.opened_at = None
                state.half_open_successes = 0
                state.half_open_in_flight = 0
            return
        state.state = CLOSED
        state.consecutive_failures = 0
        state.opened_at = None


def serialize_state(state: CircuitState) -> dict[str, Any]:
    return {
        "key": state.key,
        "state": state.state,
        "consecutiveFailures": state.consecutive_failures,
        "halfOpenInFlight": state.half_open_in_flight,
        "halfOpenSuccesses": state.half_open_successes,
        "openedAt": state.opened_at,
        "lastFailureAt": state.last_failure_at,
        "lastSuccessAt": state.last_success_at,
        "lastError": state.last_error,
    }


def _value(values: Mapping[str, Any], *keys: str, default: Any) -> Any:
    for key in keys:
        if key in values and values[key] is not None:
            return values[key]
    return default
