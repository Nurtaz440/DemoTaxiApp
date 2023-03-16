package demo.app.regstrationapp.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object MapState {
    private val mapType = MutableLiveData<MapTypes>()

    fun getMapType(): LiveData<MapTypes> {
        return mapType
    }
    fun setMapType(_mapTypes: MapTypes){
        this.mapType.value = _mapTypes
    }
}

enum class MapTypes {
    THREE_3D, TRAFFIC, SATELLITE, TRANSIT
}