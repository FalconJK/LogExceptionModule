// 獲取 Git SHA 的函數
fun getGitSha(): String {
    return try {
        // 檢查是否有未提交變更
        val hasUncommittedChanges = Runtime.getRuntime().exec("git status --porcelain").inputStream.reader().readText().trim()

        // 獲取當前 Git SHA
        val gitSha = Runtime.getRuntime().exec("git rev-parse --short HEAD").inputStream.reader().readText().trim()

        // 如果有未提交變更，添加 dirty 標記
        if (hasUncommittedChanges.isNotEmpty()) {
            "$gitSha-dirty"
        } else {
            gitSha
        }
    } catch (e: Exception) {
        "unknown"
    }
}

// 獲取構建時間
fun getBuildTime(): String {
    return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())
}

// 將函數暴露給使用此腳本的專案
extra["getGitSha"] = ::getGitSha
extra["getBuildTime"] = ::getBuildTime

