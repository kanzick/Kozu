package vn.kozu.data.lazer;

import android.os.ParcelFileDescriptor;

interface ILazerFileService {
    void destroy() = 16777114;

    String findRoot(int userId) = 1;

    ParcelFileDescriptor openFile(String path) = 2;
}
