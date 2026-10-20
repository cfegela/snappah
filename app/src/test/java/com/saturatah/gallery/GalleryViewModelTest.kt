package com.saturatah.gallery

import android.app.Application
import android.content.ContentResolver
import com.saturatah.gallery.ui.GalleryViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class GalleryViewModelTest {

    private lateinit var application: Application
    private lateinit var contentResolver: ContentResolver
    private lateinit var viewModel: GalleryViewModel

    @Before
    fun setUp() {
        application = Mockito.mock(Application::class.java)
        contentResolver = Mockito.mock(ContentResolver::class.java)
        Mockito.`when`(application.contentResolver).thenReturn(contentResolver)
        viewModel = GalleryViewModel(application)
    }

    @Test
    fun testStartSelection() {
        assertTrue(viewModel.selectedPhotoIds.value.isEmpty())

        viewModel.startSelection(101L)
        assertEquals(setOf(101L), viewModel.selectedPhotoIds.value)
    }

    @Test
    fun testTogglePhotoSelection() {
        viewModel.startSelection(101L)

        // Add 102
        viewModel.togglePhotoSelection(102L)
        assertEquals(setOf(101L, 102L), viewModel.selectedPhotoIds.value)

        // Remove 101
        viewModel.togglePhotoSelection(101L)
        assertEquals(setOf(102L), viewModel.selectedPhotoIds.value)

        // Remove 102 -> auto exits (empty set)
        viewModel.togglePhotoSelection(102L)
        assertTrue(viewModel.selectedPhotoIds.value.isEmpty())
    }

    @Test
    fun testClearMultiSelection() {
        viewModel.startSelection(101L)
        viewModel.togglePhotoSelection(102L)
        assertFalse(viewModel.selectedPhotoIds.value.isEmpty())

        viewModel.clearMultiSelection()
        assertTrue(viewModel.selectedPhotoIds.value.isEmpty())
    }
}
