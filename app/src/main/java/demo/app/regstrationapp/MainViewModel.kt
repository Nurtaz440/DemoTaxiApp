package demo.app.regstrationapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {


    private var _count = MutableLiveData(12.0)
    val count get() = _count

    fun getUpdatedCount():LiveData<Double>{
        return count
    }
    fun setZoomIn(zoomIn: Double) {
        if (zoomIn<=20){
            _count.value = zoomIn+1.0
        }else{
            this._count.value
        }

    }
    fun setZoomOut(zoomOut: Double) {
        if (zoomOut>=1){
            _count.value = zoomOut-1.0
        }else{
            this._count.value
        }

    }
}