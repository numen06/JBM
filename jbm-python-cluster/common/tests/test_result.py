from jbm_cluster_py.common.result import fail, ok


def test_ok_result_matches_jbm_shape() -> None:
    body = ok({"id": 1}, "查询成功")

    assert body == {
        "code": 200,
        "success": True,
        "message": "查询成功",
        "result": {"id": 1},
    }


def test_fail_result_matches_jbm_shape() -> None:
    body = fail(None, "失败", 400)

    assert body["code"] == 400
    assert body["success"] is False
    assert body["message"] == "失败"
    assert body["result"] is None
