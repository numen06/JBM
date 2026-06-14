from typing import Any, Dict, Optional

from pydantic import BaseModel


class ResultBody(BaseModel):
    code: int = 200
    success: bool = True
    message: str = ""
    result: Any = None

    @classmethod
    def ok(cls, result: Any = None, message: str = "操作成功") -> "ResultBody":
        return cls(code=200, success=True, message=message, result=result)

    @classmethod
    def fail(
        cls,
        result: Any = None,
        message: str = "操作失败",
        code: int = 500,
    ) -> "ResultBody":
        return cls(code=code, success=False, message=message, result=result)


def result_dict(body: ResultBody) -> Dict[str, Any]:
    if hasattr(body, "model_dump"):
        return body.model_dump()
    return body.dict()


def ok(result: Any = None, message: str = "操作成功") -> Dict[str, Any]:
    return result_dict(ResultBody.ok(result=result, message=message))


def fail(result: Any = None, message: str = "操作失败", code: int = 500) -> Dict[str, Any]:
    return result_dict(ResultBody.fail(result=result, message=message, code=code))


def page_result(contents: Optional[Any] = None, total: int = 0, curr_page: int = 1, page_size: int = 10) -> Dict[str, Any]:
    return {
        "contents": contents or [],
        "total": total,
        "currPage": curr_page,
        "pageSize": page_size,
    }
