package com.rec.gpiv

import com.rec.gpiv.player.LocalFolderFiles
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalFolderFilesTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun excludesMarkdownBeforeApplyingSubfolderLimit() {
        val root = temporaryFolder.root
        temporaryFolder.newFile("notes.md")
        temporaryFolder.newFile("README.MD")
        temporaryFolder.newFile("video.mp4")
        temporaryFolder.newFolder("child")
        temporaryFolder.newFile("child/20261113120000.Md")
        temporaryFolder.newFile("child/20260101120000.jpg")

        assertEquals(listOf("video.mp4"), LocalFolderFiles.list(root, false).map { it.name })
        assertEquals(setOf("video.mp4", "20260101120000.jpg"),
            LocalFolderFiles.list(root, true, 1).map { it.name }.toSet())
    }

    @Test fun limitsEachImmediateSubfolderInPlayerOrder() {
        val root = temporaryFolder.root
        temporaryFolder.newFile("root.jpg")
        for (folder in listOf("sibling", "child")) {
            temporaryFolder.newFolder(folder)
            temporaryFolder.newFile("$folder/gpivtags.csv")
            temporaryFolder.newFolder(folder, "deeper")
            temporaryFolder.newFile("$folder/deeper/excluded.jpg")
            for (day in 1..5) {
                temporaryFolder.newFile("$folder/2026010${day}120000.jpg")
            }
        }
        temporaryFolder.newFolder("small")
        temporaryFolder.newFile("small/only.jpg")

        val paths = LocalFolderFiles.list(root, true, 3).map { it.relativeTo(root).path }

        val childFiles = (5 downTo 3).map { day -> "child/2026010${day}120000.jpg" }
        val siblingFiles = (5 downTo 3).map { day -> "sibling/2026010${day}120000.jpg" }
        assertEquals(childFiles + siblingFiles + "small/only.jpg" + "root.jpg", paths)
        assertEquals(12, LocalFolderFiles.list(root, true).size)
    }

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
