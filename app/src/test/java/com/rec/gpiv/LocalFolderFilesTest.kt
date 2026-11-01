package com.rec.gpiv

import com.rec.gpiv.player.LocalFolderFiles
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalFolderFilesTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun includesOnlyImmediateSubfoldersAndKeepsDuplicateNames() {
        val root = temporaryFolder.root
        temporaryFolder.newFile("same.jpg")
        temporaryFolder.newFile("gpivtags.csv")
        temporaryFolder.newFolder("child")
        temporaryFolder.newFile("child/same.jpg")
        temporaryFolder.newFile("child/gpivtags.csv")
        temporaryFolder.newFolder("child", "deeper")
        temporaryFolder.newFile("child/deeper/excluded.jpg")
        temporaryFolder.newFolder("empty")
        temporaryFolder.newFolder("sibling")
        temporaryFolder.newFile("sibling/video.mp4")

        val paths = LocalFolderFiles.list(root, true).map { it.relativeTo(root).path }.toSet()
        assertEquals(setOf("same.jpg", "child/same.jpg", "sibling/video.mp4"), paths)
        assertEquals(listOf("same.jpg"), LocalFolderFiles.list(root, false).map { it.name })
    }
}
