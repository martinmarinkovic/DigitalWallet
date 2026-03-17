package com.threemdroid.digitalwallet.feature.addcard

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PhotoScanModule {
    @Binds
    abstract fun bindPhotoScanExtractor(
        extractor: MlKitPhotoScanExtractor
    ): PhotoScanExtractor
}
