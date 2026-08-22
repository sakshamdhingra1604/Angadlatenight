package com.sonusid.localvpntunnel

object MLBridge {
    init { System.loadLibrary("api") }
    external fun initEngine(flowModelBytes: ByteArray, shieldnetModelBytes: ByteArray): Boolean
    external fun analyzePacket(flowId: Long, packetBytes: ByteArray, isUpload: Boolean): Float
}
