package jp.araobp.camera.util

class Fps {

    val lastFrameTime = longArrayOf(0L, 0L, 0L, 0L, 0L)

    fun update(): Double {
        lastFrameTime[4] = lastFrameTime[3]
        lastFrameTime[3] = lastFrameTime[2]
        lastFrameTime[2] = lastFrameTime[1]
        lastFrameTime[1] = lastFrameTime[0]
        lastFrameTime[0] = System.currentTimeMillis()

        val diff = longArrayOf(0L, 0L, 0L, 0L)
        diff[3] = lastFrameTime[4] - lastFrameTime[3]
        diff[2] = lastFrameTime[3] - lastFrameTime[2]
        diff[1] = lastFrameTime[2] - lastFrameTime[1]
        diff[0] = lastFrameTime[1] - lastFrameTime[0]
        val avg = diff.average()
        return (1000.0/avg).roundToTheNth(1)
    }

}