package com.anytvplayer.ios.data

import kotlin.test.Test
import kotlin.test.assertEquals

class Sha256Test {

    @Test
    fun emptyStringMatchesKnownDigest() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Sha256.hexDigest("")
        )
    }

    @Test
    fun abcMatchesKnownDigest() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Sha256.hexDigest("abc")
        )
    }

    @Test
    fun multiBlockInputMatchesKnownDigest() {
        assertEquals(
            "cf5b16a778af8380036ce59e7b0492370b249b11e8f07a51afac45037afee9d1",
            Sha256.hexDigest(
                "abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmn" +
                    "hijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu"
            )
        )
    }

    @Test
    fun recoveryTokenShapeIsSixtyFourHexChars() {
        val digest = Sha256.hexDigest("vendor-id|com.anytvplayer.ios|anytv-reinstall-v1")
        assertEquals(64, digest.length)
        assertEquals(true, digest.all { it in "0123456789abcdef" })
    }
}
