package com.example.todo.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit test cho [sessionExpiredMessageFor] (NavGraph.kt) — TC-SESS-01/02 (docs/Test_Scenarios_ToDo_Android.md):
 * phân biệt message khi 401 xảy ra ở form Tạo/Sửa task (mất dữ liệu đang nhập) với mọi route khác.
 */
class NavGraphSessionExpiredMessageTest {

    @Test
    fun `route la null (vd chua co destination) thi dung message thuong`() {
        assertEquals(
            "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại",
            sessionExpiredMessageFor(null)
        )
    }

    @Test
    fun `route TASK_LIST thi dung message thuong (TC-SESS-01)`() {
        assertEquals(
            "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại",
            sessionExpiredMessageFor(Routes.TASK_LIST)
        )
    }

    @Test
    fun `route CREATE_TASK thi dung message canh bao mat du lieu (TC-SESS-02)`() {
        assertEquals(
            "Phiên đăng nhập đã hết hạn. Dữ liệu bạn đang nhập sẽ không được lưu, vui lòng đăng nhập lại.",
            sessionExpiredMessageFor(Routes.CREATE_TASK)
        )
    }

    @Test
    fun `route EDIT_TASK (pattern co placeholder taskId) thi dung message canh bao mat du lieu (TC-SESS-02)`() {
        assertEquals(
            "Phiên đăng nhập đã hết hạn. Dữ liệu bạn đang nhập sẽ không được lưu, vui lòng đăng nhập lại.",
            sessionExpiredMessageFor(Routes.EDIT_TASK)
        )
    }

    @Test
    fun `route LOGIN REGISTER hay cac man auth khac thi dung message thuong`() {
        assertEquals(
            "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại",
            sessionExpiredMessageFor(Routes.REGISTER)
        )
    }
}
