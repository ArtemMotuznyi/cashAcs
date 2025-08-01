package ua.developer.artemmotuznyi.ukrsibparser.ui

import kotlinx.html.*

fun HTML.generateAuthFormHtml(onError: () -> Unit) = this.apply {
    head {
        title("Cash ACS - Authentication")
        style {
            unsafe {
                +"""
                        body { font-family: Arial, sans-serif; margin: 40px; }
                        .container { max-width: 400px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px; }
                        .form-group { margin-bottom: 15px; }
                        label { display: block; margin-bottom: 5px; font-weight: bold; }
                        input[type="text"], input[type="password"] { width: 100%; padding: 8px; border: 1px solid #ccc; border-radius: 4px; }
                        button { background-color: #007bff; color: white; padding: 10px 20px; border: none; border-radius: 4px; cursor: pointer; }
                        button:hover { background-color: #0056b3; }
                        .error { color: red; margin-top: 10px; }
                        """.trimIndent()
            }
        }
    }
    body {
        div("container") {
            h2 { +"Cash ACS Authentication" }
            form(action = "/auth", method = FormMethod.post) {
                div("form-group") {
                    label { +"Username:" }
                    input(type = InputType.text, name = "username") {
                        required = true
                    }
                }
                div("form-group") {
                    label { +"Password:" }
                    input(type = InputType.password, name = "password") {
                        required = true
                    }
                }
                button(type = ButtonType.submit) { +"Login" }
            }
        }
    }
}

fun HTML.generateErrorHtml(error: String) = this.apply {
    head {
        title("Error")
        style {
            unsafe {
                +"""
                        body { font-family: Arial, sans-serif; margin: 40px; }
                        .error { color: red; }
                        """.trimIndent()
            }
        }
    }
    body {
        div("error") {
            +when (error) {
                "invalid" -> "Invalid username or password"
                "auth_failed" -> "Gmail authentication failed"
                else -> "An error occurred"
            }
        }
    }
}