from __future__ import annotations

from typing import Any, Dict, List, Optional

from pydantic import Field

from jbm_cluster_py.common.masterdata import LooseModel


class PushMessageEvent(LooseModel):
    event_type: Optional[str] = Field(default=None, alias="eventType")
    push_way: Optional[str] = Field(default="internal", alias="pushWay")
    msg_id: Optional[str] = Field(default=None, alias="msgId")
    rec_user_id: Optional[int] = Field(default=None, alias="recUserId")
    rec_user_ids: List[int] = Field(default_factory=list, alias="recUserIds")
    send_user_id: Optional[int] = Field(default=None, alias="sendUserId")
    sys_msg: Optional[bool] = Field(default=False, alias="sysMsg")
    title: Optional[str] = None
    content: Optional[str] = None
    level: Optional[int] = 0
    push_msg_type: Optional[str] = Field(default="notification", alias="pushMsgType")
    url: Optional[str] = None
    extend: Optional[Dict[str, Any]] = None
    show_in_message_center: Optional[bool] = Field(default=True, alias="showInMessageCenter")
    test_run_id: Optional[str] = Field(default=None, alias="testRunId")
    message_index: Optional[int] = Field(default=1, alias="messageIndex")
