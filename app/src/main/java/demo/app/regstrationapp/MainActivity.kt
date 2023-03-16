package demo.app.regstrationapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.timepicker.TimeFormat
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.atmosphere.generated.atmosphere
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.layers.generated.fillExtrusionLayer
import com.mapbox.maps.extension.style.layers.generated.skyLayer
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.layers.properties.generated.SkyType
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.rasterDemSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.extension.style.terrain.generated.terrain
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.lifecycle.requireMapboxNavigation
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.*
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import com.mapbox.navigation.ui.voice.model.SpeechVolume
import demo.app.regstrationapp.databinding.ActivityMainBinding
import demo.app.regstrationapp.util.LocationPermissionHelper
import demo.app.regstrationapp.util.MapState
import demo.app.regstrationapp.util.MapTypes
import demo.app.regstrationapp.util.PrefsHelper
import java.lang.ref.WeakReference
import java.util.*


class MainActivity : AppCompatActivity(), OnMapClickListener {
    private lateinit var locationPermissionHelper: LocationPermissionHelper
    private var _binding: ActivityMainBinding? = null
    val binding get() = _binding!!

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        binding.mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        binding.mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
        binding.mapView.gestures.focalPoint = binding.mapView.getMapboxMap().pixelForCoordinate(it)
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            onCameraTrackingDismissed()
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }

    //  var prefs= PrefsHelper(PreferenceManager.getDefaultSharedPreferences(this))
    private val prefs by lazy {
        PrefsHelper(
            applicationContext.getSharedPreferences(
                MapTypes.TRANSIT.name,
                MODE_PRIVATE
            )
        )
    }

    private val viewModel by lazy { MainViewModel() }
    private var listOfPointsList = mutableListOf(mutableListOf<Point>())
    private var pointController = 1
    private var drawController = 0
    var poligon: MutableLiveData<Int> = MutableLiveData(0)
    private var countList: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        installSplashScreen()
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val mapType = prefs.mapType

        if (mapType != null) {
            when (mapType) {
                MapTypes.TRANSIT.name -> MapState.setMapType(MapTypes.TRANSIT)
                MapTypes.THREE_3D.name -> MapState.setMapType(MapTypes.THREE_3D)
                MapTypes.SATELLITE.name -> MapState.setMapType(MapTypes.SATELLITE)
                MapTypes.TRAFFIC.name -> MapState.setMapType(MapTypes.TRAFFIC)
            }
        } else {
            MapState.setMapType(MapTypes.TRANSIT)
            prefs.mapType = MapTypes.TRANSIT.name
        }

        binding.cv3d.setOnClickListener {
            prefs.mapType = MapTypes.THREE_3D.name
            MapState.setMapType(MapTypes.THREE_3D)
            binding.cv3d.strokeWidth = 2
            binding.cv3d.strokeColor = ContextCompat.getColor(this, R.color.blue)
            binding.cvTransit.strokeColor = ContextCompat.getColor(this, R.color.transparent)
            binding.cvSetelit.strokeColor = ContextCompat.getColor(this, R.color.transparent)
            binding.cvTraffic.strokeColor = ContextCompat.getColor(this, R.color.transparent)
        }
        binding.cvTransit.setOnClickListener {
            prefs.mapType = MapTypes.TRANSIT.name
            MapState.setMapType(MapTypes.TRANSIT)
            binding.cvTransit.strokeWidth = 2
            binding.cvTransit.strokeColor = ContextCompat.getColor(this, R.color.blue)
            binding.cv3d.strokeColor = ContextCompat.getColor(this, R.color.transparent)
            binding.cvSetelit.strokeColor = ContextCompat.getColor(this, R.color.transparent)
            binding.cvTraffic.strokeColor = ContextCompat.getColor(this, R.color.transparent)
        }
        binding.cvSetelit.setOnClickListener {
            prefs.mapType = MapTypes.SATELLITE.name
            MapState.setMapType(MapTypes.SATELLITE)
            binding.cvSetelit.strokeWidth = 2
            binding.cvSetelit.strokeColor = ContextCompat.getColor(this, R.color.blue)
            binding.cvTransit.strokeColor = ContextCompat.getColor(this, R.color.transparent)
            binding.cv3d.strokeColor = ContextCompat.getColor(this, R.color.transparent)
            binding.cvTraffic.strokeColor = ContextCompat.getColor(this, R.color.transparent)
        }
        binding.cvTraffic.setOnClickListener {
            prefs.mapType = MapTypes.TRAFFIC.name
            MapState.setMapType(MapTypes.TRAFFIC)
            binding.cvTraffic.strokeWidth = 2
            binding.cvTraffic.strokeColor = ContextCompat.getColor(this, R.color.blue)
            binding.cvTransit.strokeColor = ContextCompat.getColor(this, R.color.transparent)
            binding.cv3d.strokeColor = ContextCompat.getColor(this, R.color.transparent)
            binding.cvSetelit.strokeColor = ContextCompat.getColor(this, R.color.transparent)
        }

        MapState.getMapType().observe(this, Observer { mapType ->
            val nightModeFlags: Int = getResources().getConfiguration().uiMode and
                    Configuration.UI_MODE_NIGHT_MASK
            when (nightModeFlags) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    binding.mapView.getMapboxMap().loadStyleUri(Style.DARK)
                    binding.cvNavigate.setOnClickListener {
                        binding.mapView.getMapboxMap()
                            .loadStyleUri(NavigationStyles.NAVIGATION_NIGHT_STYLE)
                    }
                    getOptions()
                }
                else -> {
                    getOptionsLight()

                    when (mapType) {

                        MapTypes.TRANSIT -> {
                            binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)
                            binding.cvNavigate.setOnClickListener {
                                binding.mapView.getMapboxMap()
                                    .loadStyleUri(NavigationStyles.NAVIGATION_DAY_STYLE)
                            }
                            onMapReady()
                        }
                        MapTypes.THREE_3D -> {
                            build3D()
                            binding.cvNavigate.setOnClickListener {
                                showNavigation()
                            }
                            onMapReady()
                        }
                        MapTypes.SATELLITE -> {
                            binding.mapView.getMapboxMap().loadStyleUri(Style.SATELLITE)
                            binding.cvNavigate.setOnClickListener {
                                binding.mapView.getMapboxMap()
                                    .loadStyleUri(NavigationStyles.NAVIGATION_DAY_STYLE)
                            }
                            onMapReady()
                        }
                        MapTypes.TRAFFIC -> {
                            binding.mapView.getMapboxMap().loadStyleUri(Style.TRAFFIC_DAY)
                            binding.cvNavigate.setOnClickListener {
                                binding.mapView.getMapboxMap()
                                    .loadStyleUri(NavigationStyles.NAVIGATION_DAY_STYLE)
                            }
                            onMapReady()
                        }
                    }

                }
            }
        })

        binding.mapView.scalebar.enabled = false
        binding.mapView.compass.updateSettings {
            enabled = false
        }

        locationPermissionHelper = LocationPermissionHelper(WeakReference(this))
        locationPermissionHelper.checkPermissions {
            onMapReady()
        }

        viewModel.getUpdatedCount().observe(this@MainActivity, Observer { zoom ->
            Log.d("Zooming", zoom.toString())
            binding.cvPlus.setOnClickListener {
                viewModel.setZoomIn(zoom)
                binding.mapView.getMapboxMap().setCamera(
                    CameraOptions.Builder()
                        .zoom(viewModel.count.value)
                        .build()
                )
            }
            binding.cvMinus.setOnClickListener {
                Log.d("Zooming", zoom.toString())
                viewModel.setZoomOut(zoom)
                binding.mapView.getMapboxMap().setCamera(
                    CameraOptions.Builder()
                        .zoom(viewModel.count.value)
                        .build()
                )
            }
        })
        // initialize Navigation Camera
        viewportDataSource = MapboxNavigationViewportDataSource(binding.mapView.getMapboxMap())
        navigationCamera = NavigationCamera(
            binding.mapView.getMapboxMap(),
            binding.mapView.camera,
            viewportDataSource
        )
        // set the animations lifecycle listener to ensure the NavigationCamera stops
        // automatically following the user location when the map is interacted with
        binding.mapView.camera.addCameraAnimationsLifecycleListener(
            NavigationBasicGesturesHandler(navigationCamera)
        )
        navigationCamera.registerNavigationCameraStateChangeObserver { navigationCameraState ->
            // shows/hide the recenter button depending on the camera state
            when (navigationCameraState) {
                NavigationCameraState.TRANSITION_TO_FOLLOWING,
                NavigationCameraState.FOLLOWING -> binding.recenter.visibility = View.INVISIBLE
                NavigationCameraState.TRANSITION_TO_OVERVIEW,
                NavigationCameraState.OVERVIEW,
                NavigationCameraState.IDLE -> binding.recenter.visibility = View.VISIBLE
            }
        }
        // set the padding values depending on screen orientation and visible view layout
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.overviewPadding = landscapeOverviewPadding
        } else {
            viewportDataSource.overviewPadding = overviewPadding
        }
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.followingPadding = landscapeFollowingPadding
        } else {
            viewportDataSource.followingPadding = followingPadding
        }

        // make sure to use the same DistanceFormatterOptions across different features
        val distanceFormatterOptions = DistanceFormatterOptions.Builder(this).build()

        // initialize maneuver api that feeds the data to the top banner maneuver view
        maneuverApi = MapboxManeuverApi(
            MapboxDistanceFormatter(distanceFormatterOptions)
        )

        // initialize bottom progress view
        tripProgressApi = MapboxTripProgressApi(
            TripProgressUpdateFormatter.Builder(this)
                .distanceRemainingFormatter(
                    DistanceRemainingFormatter(distanceFormatterOptions)
                )
                .timeRemainingFormatter(
                    TimeRemainingFormatter(this)
                )
                .percentRouteTraveledFormatter(
                    PercentDistanceTraveledFormatter()
                )
                .estimatedTimeToArrivalFormatter(
                    EstimatedTimeToArrivalFormatter(this, TimeFormat.CLOCK_12H)
                )
                .build()
        )

        // initialize voice instructions api and the voice instruction player
        speechApi = MapboxSpeechApi(
            this,
            getString(R.string.mapbox_access_token),
            Locale.US.language
        )
        voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(
            this,
            getString(R.string.mapbox_access_token),
            Locale.US.language
        )

        // initialize route line, the withRouteLineBelowLayerId is specified to place
        // the route line below road labels layer on the map
        // the value of this option will depend on the style that you are using
        // and under which layer the route line should be placed on the map layers stack
        val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(this)
            .withRouteLineBelowLayerId("road-label-navigation")
            .build()
        routeLineApi = MapboxRouteLineApi(mapboxRouteLineOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)

        // initialize maneuver arrow view to draw arrows on the map
        val routeArrowOptions = RouteArrowOptions.Builder(this).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)


        binding.mapView.getMapboxMap().addOnMapClickListener(this)

        // initialize view interactions
        binding.stop.setOnClickListener {
            clearRouteAndStopNavigation()
            showUI()
        }
        binding.recenter.setOnClickListener {
            navigationCamera.requestNavigationCameraToFollowing()
            binding.routeOverview.showTextAndExtend(BUTTON_ANIMATION_DURATION)
        }
        binding.routeOverview.setOnClickListener {
            navigationCamera.requestNavigationCameraToOverview()
            binding.recenter.showTextAndExtend(BUTTON_ANIMATION_DURATION)
        }
        binding.soundButton.setOnClickListener {
            // mute/unmute voice instructions
            isVoiceInstructionsMuted = !isVoiceInstructionsMuted
        }

        // set initial sounds button state
        binding.soundButton.unmute()
    }


    fun build3D() {
        // object or in the same activity which contains the mapview.

        binding.mapView.getMapboxMap().loadStyle(
            styleExtension = style(getString(R.string.mapbox_3d_style)) {
                +terrain("TERRAIN_SOURCE").exaggeration(1.5)
                +skyLayer("sky") {
                    skyType(SkyType.ATMOSPHERE)
                    skyAtmosphereSun(listOf(-50.0, 90.2))
                }
                +atmosphere {}
                +projection(ProjectionName.GLOBE)
                +fillExtrusionLayer("3d-buildings", "composite") {
                    fillExtrusionHeight(
                        get("height")
                    )
                }
            }
        ) {
            it.addSource(rasterDemSource("TERRAIN_SOURCE") {
                url("mapbox://mapbox.terrain-rgb")
                tileSize(512)
            })

        }

    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    private fun onMapReady() {

        binding.mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .pitch(0.0)
                .zoom(12.0)
                .build()
        )

        setupGesturesListener()
    }

    private fun setupGesturesListener() {
        binding.mapView.gestures.addOnMoveListener(onMoveListener)
    }


    private fun onCameraTrackingDismissed() {
        //    Toast.makeText(this, "onCameraTrackingDismissed", Toast.LENGTH_SHORT).show()
        binding.mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        binding.mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        binding.mapView.gestures.removeOnMoveListener(onMoveListener)
        mapboxReplayer.finish()
        maneuverApi.cancel()
        routeLineApi.cancel()
        routeLineView.cancel()
        speechApi.cancel()
        voiceInstructionsPlayer.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun getOptions() {
        //hide
        binding.cvSetelit.visibility = View.GONE
        binding.cv3d.visibility = View.GONE
        binding.cvTransit.visibility = View.GONE
        binding.cvTraffic.visibility = View.GONE

        //change color
        binding.cvRecover.setCardBackgroundColor(
            ContextCompat.getColor(
                this,
                R.color.white_night_mode
            )
        )
        binding.cvCharge.setCardBackgroundColor(
            ContextCompat.getColor(
                this,
                R.color.white_night_mode
            )
        )
        binding.cvNotif.setCardBackgroundColor(
            ContextCompat.getColor(
                this,
                R.color.white_night_mode
            )
        )
        binding.cvPlus.setCardBackgroundColor(
            ContextCompat.getColor(
                this,
                R.color.white_night_mode
            )
        )
        binding.cvMinus.setCardBackgroundColor(
            ContextCompat.getColor(
                this,
                R.color.white_night_mode
            )
        )
        binding.cvNavigate.setCardBackgroundColor(
            ContextCompat.getColor(
                this,
                R.color.white_night_mode
            )
        )
        binding.rlDark.setBackgroundColor(ContextCompat.getColor(this, R.color.white_night_mode))

        binding.cvZakaz.setBackgroundDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.cv_back_dark
            )
        )
        binding.cvBardior.setBackgroundDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.cv_back_dark
            )
        )
        binding.cvTariff.setBackgroundDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.cv_back_dark
            )
        )

        binding.tvOrder.setTextColor(ContextCompat.getColor(this, R.color.white_night_mode))
        binding.tvRocket.setTextColor(ContextCompat.getColor(this, R.color.white_night_mode))
        binding.tvTraffic.setTextColor(ContextCompat.getColor(this, R.color.white_night_mode))

        //change icon
        binding.ivRocket.setImageResource(R.drawable.ic_rocket_dark)
        binding.ivTariff.setImageResource(R.drawable.ic_tarif_dark)
        binding.ivZakaz.setImageResource(R.drawable.seven)
    }

    fun getOptionsLight() {
        binding.cvZakaz.setBackgroundDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.cv_back_light
            )
        )
        binding.cvBardior.setBackgroundDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.cv_back_light
            )
        )
        binding.cvTariff.setBackgroundDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.cv_back_light
            )
        )
    }

    fun showNavigation() {
        binding.mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .pitch(75.0)
                .zoom(12.0)
                .bearing(-17.6)
                .build()
        )
    }


    fun setPointController(pointController: Int) {
        this.pointController = pointController
    }

    fun setDrawController(drawController: Int) {
        this.drawController = drawController
    }


    fun setMarker(point: Point) {
        bitmapFromDrawableRes(
            this@MainActivity,
            R.drawable.map_pin
        )?.let {
            val annotationApi = binding.mapView.annotations
            val pointAnnotationManager = annotationApi.createPointAnnotationManager(binding.mapView)
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(it)
                .withDraggable(true)
            pointAnnotationManager.create(pointAnnotationOptions)
        }
    }


    override fun onMapClick(point: Point): Boolean {
        windowStyle()
        val nightModeFlags: Int = getResources().getConfiguration().uiMode and
                Configuration.UI_MODE_NIGHT_MASK
        when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> {
                binding.mapView.getMapboxMap().loadStyleUri(NavigationStyles.NAVIGATION_NIGHT_STYLE)
                getOptions()
            }
            else -> {
                binding.mapView.getMapboxMap().loadStyleUri(NavigationStyles.NAVIGATION_DAY_STYLE)
            }
        }

        var point_var = point
        if (pointController == 1) {
            if (drawController == 1 && listOfPointsList[countList].size < 2) {
                point_var = listOfPointsList[countList][0]
                poligon.value = 1
                setDrawController(0)
                setPointController(0)
            }
            listOfPointsList[countList].add(point_var)
            setMarker(point_var)
            findRoute(point_var)
            ++pointController
        }
        return false
    }

    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
        if (sourceDrawable == null) {
            return null
        }
        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else {
// copying drawable object to not manipulate on the same reference
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    private companion object {
        private const val BUTTON_ANIMATION_DURATION = 1500L
    }

    /**
     * Debug tool used to play, pause and seek route progress events that can be used to produce mocked location updates along the route.
     */
    private val mapboxReplayer = MapboxReplayer()

    /**
     * Debug tool that mocks location updates with an input from the [mapboxReplayer].
     */
    private val replayLocationEngine = ReplayLocationEngine(mapboxReplayer)

    /**
     * Debug observer that makes sure the replayer has always an up-to-date information to generate mock updates.
     */
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)


    /**
     * Used to execute camera transitions based on the data generated by the [viewportDataSource].
     * This includes transitions from route overview to route following and continuously updating the camera as the location changes.
     */
    private lateinit var navigationCamera: NavigationCamera

    /**
     * Produces the camera frames based on the location and routing data for the [navigationCamera] to execute.
     */
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource

    /*
    * Below are generated camera padding values to ensure that the route fits well on screen while
    * other elements are overlaid on top of the map (including instruction view, buttons, etc.)
    */
    private val pixelDensity = Resources.getSystem().displayMetrics.density
    private val overviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            140.0 * pixelDensity,
            40.0 * pixelDensity,
            120.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeOverviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            20.0 * pixelDensity
        )
    }
    private val followingPadding: EdgeInsets by lazy {
        EdgeInsets(
            180.0 * pixelDensity,
            40.0 * pixelDensity,
            150.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeFollowingPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }

    /**
     * Generates updates for the [MapboxManeuverView] to display the upcoming maneuver instructions
     * and remaining distance to the maneuver point.
     */
    private lateinit var maneuverApi: MapboxManeuverApi

    /**
     * Generates updates for the [MapboxTripProgressView] that include remaining time and distance to the destination.
     */
    private lateinit var tripProgressApi: MapboxTripProgressApi

    /**
     * Generates updates for the [routeLineView] with the geometries and properties of the routes that should be drawn on the map.
     */
    private lateinit var routeLineApi: MapboxRouteLineApi

    /**
     * Draws route lines on the map based on the data from the [routeLineApi]
     */
    private lateinit var routeLineView: MapboxRouteLineView

    /**
     * Generates updates for the [routeArrowView] with the geometries and properties of maneuver arrows that should be drawn on the map.
     */
    private val routeArrowApi: MapboxRouteArrowApi = MapboxRouteArrowApi()

    /**
     * Draws maneuver arrows on the map based on the data [routeArrowApi].
     */
    private lateinit var routeArrowView: MapboxRouteArrowView

    /**
     * Stores and updates the state of whether the voice instructions should be played as they come or muted.
     */
    private var isVoiceInstructionsMuted = false
        set(value) {
            field = value
            if (value) {
                binding.soundButton.muteAndExtend(BUTTON_ANIMATION_DURATION)
                voiceInstructionsPlayer.volume(SpeechVolume(0f))
            } else {
                binding.soundButton.unmuteAndExtend(BUTTON_ANIMATION_DURATION)
                voiceInstructionsPlayer.volume(SpeechVolume(1f))
            }
        }

    /**
     * Extracts message that should be communicated to the driver about the upcoming maneuver.
     * When possible, downloads a synthesized audio file that can be played back to the driver.
     */
    private lateinit var speechApi: MapboxSpeechApi

    /**
     * Plays the synthesized audio files with upcoming maneuver instructions
     * or uses an on-device Text-To-Speech engine to communicate the message to the driver.
     * NOTE: do not use lazy initialization for this class since it takes some time to initialize
     * the system services required for on-device speech synthesis. With lazy initialization
     * there is a high risk that said services will not be available when the first instruction
     * has to be played. [MapboxVoiceInstructionsPlayer] should be instantiated in
     * `Activity#onCreate`.
     */
    private lateinit var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer

    /**
     * Observes when a new voice instruction should be played.
     */
    private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
        speechApi.generate(voiceInstructions, speechCallback)
    }

    /**
     * Based on whether the synthesized audio file is available, the callback plays the file
     * or uses the fall back which is played back using the on-device Text-To-Speech engine.
     */
    private val speechCallback =
        MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> { expected ->
            expected.fold(
                { error ->
// play the instruction via fallback text-to-speech engine
                    voiceInstructionsPlayer.play(
                        error.fallback,
                        voiceInstructionsPlayerCallback
                    )
                },
                { value ->
// play the sound file from the external generator
                    voiceInstructionsPlayer.play(
                        value.announcement,
                        voiceInstructionsPlayerCallback
                    )
                }
            )
        }

    /**
     * When a synthesized audio file was downloaded, this callback cleans up the disk after it was played.
     */
    private val voiceInstructionsPlayerCallback =
        MapboxNavigationConsumer<SpeechAnnouncement> { value ->
// remove already consumed file to free-up space
            speechApi.clean(value)
        }

    /**
     * [NavigationLocationProvider] is a utility class that helps to provide location updates generated by the Navigation SDK
     * to the Maps SDK in order to update the user location indicator on the map.
     */
    private val navigationLocationProvider = NavigationLocationProvider()

    /**
     * Gets notified with location updates.
     *
     * Exposes raw updates coming directly from the location services
     * and the updates enhanced by the Navigation SDK (cleaned up and matched to the road).
     */
    private val locationObserver = object : LocationObserver {
        var firstLocationUpdateReceived = false

        override fun onNewRawLocation(rawLocation: Location) {
// not handled
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation
// update location puck's position on the map
            navigationLocationProvider.changePosition(
                location = enhancedLocation,
                keyPoints = locationMatcherResult.keyPoints,
            )

// update camera position to account for new location
            viewportDataSource.onLocationChanged(enhancedLocation)
            viewportDataSource.evaluate()

// if this is the first location update the activity has received,
// it's best to immediately move the camera to the current user location
            if (!firstLocationUpdateReceived) {
                firstLocationUpdateReceived = true
                navigationCamera.requestNavigationCameraToOverview(
                    stateTransitionOptions = NavigationCameraTransitionOptions.Builder()
                        .maxDuration(0) // instant transition
                        .build()
                )
            }
        }
    }

    /**
     * Gets notified with progress along the currently active route.
     */
    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
// update the camera position to account for the progressed fragment of the route
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

// draw the upcoming maneuver arrow on the map
        val style = binding.mapView.getMapboxMap().getStyle()
        if (style != null) {
            val maneuverArrowResult = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
            routeArrowView.renderManeuverUpdate(style, maneuverArrowResult)
        }

// update top banner with maneuver instructions
        val maneuvers = maneuverApi.getManeuvers(routeProgress)
        maneuvers.fold(
            { error ->
                Toast.makeText(
                    this@MainActivity,
                    error.errorMessage,
                    Toast.LENGTH_SHORT
                ).show()
            },
            {
                binding.maneuverView.visibility = View.VISIBLE
                binding.maneuverView.renderManeuvers(maneuvers)
            }
        )

// update bottom trip progress summary
        binding.tripProgressView.render(
            tripProgressApi.getTripProgress(routeProgress)
        )
    }

    /**
     * Gets notified whenever the tracked routes change.
     *
     * A change can mean:
     * - routes get changed with [MapboxNavigation.setRoutes]
     * - routes annotations get refreshed (for example, congestion annotation that indicate the live traffic along the route)
     * - driver got off route and a reroute was executed
     */
    private val routesObserver = RoutesObserver { routeUpdateResult ->
        if (routeUpdateResult.navigationRoutes.isNotEmpty()) {
// generate route geometries asynchronously and render them
            routeLineApi.setNavigationRoutes(
                routeUpdateResult.navigationRoutes
            ) { value ->
                binding.mapView.getMapboxMap().getStyle()?.apply {
                    routeLineView.renderRouteDrawData(this, value)
                }
            }

// update the camera position to account for the new route
            viewportDataSource.onRouteChanged(routeUpdateResult.navigationRoutes.first())
            viewportDataSource.evaluate()
        } else {
// remove the route line and route arrow from the map
            val style = binding.mapView.getMapboxMap().getStyle()
            if (style != null) {
                routeLineApi.clearRouteLine { value ->
                    routeLineView.renderClearRouteLineValue(
                        style,
                        value
                    )
                }
                routeArrowView.render(style, routeArrowApi.clearArrows())
            }

// remove the route reference from camera position evaluations
            viewportDataSource.clearRouteData()
            viewportDataSource.evaluate()
        }
    }

    private val mapboxNavigation: MapboxNavigation by requireMapboxNavigation(
        onResumedObserver = object : MapboxNavigationObserver {
            @SuppressLint("MissingPermission")
            override fun onAttached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.registerRoutesObserver(routesObserver)
                mapboxNavigation.registerLocationObserver(locationObserver)
                mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
                mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
                mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
// start the trip session to being receiving location updates in free drive
// and later when a route is set also receiving route progress updates
                mapboxNavigation.startTripSession()
            }

            override fun onDetached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.unregisterRoutesObserver(routesObserver)
                mapboxNavigation.unregisterLocationObserver(locationObserver)
                mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
                mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
                mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
            }
        },
        onInitialize = this::initNavigation
    )


    private fun initNavigation() {
        MapboxNavigationApp.setup(
            NavigationOptions.Builder(this)
                .accessToken(getString(R.string.mapbox_access_token))
// comment out the location engine setting block to disable simulation
                .locationEngine(replayLocationEngine)
                .build()
        )

// initialize location puck
        binding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.yellow_rotate_resize
                ),

                scaleExpression = interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(20.0)
                        literal(1.0)
                    }
                }.toJson()
            )
            enabled = true
        }
        val locationComponentPlugin = binding.mapView.location

        locationComponentPlugin.addOnIndicatorPositionChangedListener(
            onIndicatorPositionChangedListener
        )
        locationComponentPlugin.addOnIndicatorBearingChangedListener(
            onIndicatorBearingChangedListener
        )
        replayOriginLocation()
    }

    private fun replayOriginLocation() {
        mapboxReplayer.pushEvents(
            listOf(
                ReplayRouteMapper.mapToUpdateLocation(
                    Date().time.toDouble(),
                    Point.fromLngLat(-122.39726512303575, 37.785128345296805)
                )
            )
        )
        mapboxReplayer.playFirstLocation()
        mapboxReplayer.playbackSpeed(3.0)
    }

    private fun findRoute(destination: Point) {
        val originLocation = navigationLocationProvider.lastLocation
        val originPoint = originLocation?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        } ?: return

// execute a route request
// it's recommended to use the
// applyDefaultNavigationOptions and applyLanguageAndVoiceUnitOptions
// that make sure the route request is optimized
// to allow for support of all of the Navigation SDK features
        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(this)
                .coordinatesList(listOf(originPoint, destination))
// provide the bearing for the origin of the request to ensure
// that the returned route faces in the direction of the current user movement
                .bearingsList(
                    listOf(
                        Bearing.builder()
                            .angle(originLocation.bearing.toDouble())
                            .degrees(45.0)
                            .build(),
                        null
                    )
                )
                .layersList(listOf(mapboxNavigation.getZLevel(), null))
                .build(),
            object : NavigationRouterCallback {
                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
// no impl
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
// no impl
                }

                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    setRouteAndStartNavigation(routes)
                }
            }
        )
    }

    private fun setRouteAndStartNavigation(routes: List<NavigationRoute>) {
// set routes, where the first route in the list is the primary route that
// will be used for active guidance
        mapboxNavigation.setNavigationRoutes(routes)

// show UI elements
        binding.soundButton.visibility = View.VISIBLE
        binding.routeOverview.visibility = View.VISIBLE
        binding.tripProgressCard.visibility = View.VISIBLE

// move the camera to overview when new route is available
        navigationCamera.requestNavigationCameraToOverview()
    }

    private fun clearRouteAndStopNavigation() {
// clear
        mapboxNavigation.setNavigationRoutes(listOf())

// stop simulation
        mapboxReplayer.stop()
        pointController = 1

// hide UI elements
        binding.soundButton.visibility = View.INVISIBLE
        binding.maneuverView.visibility = View.INVISIBLE
        binding.routeOverview.visibility = View.INVISIBLE
        binding.tripProgressCard.visibility = View.INVISIBLE
    }

    fun windowStyle() {

        binding.llMapStyles.visibility = View.GONE
        binding.cvRecover.visibility = View.GONE
        binding.cvNotif.visibility = View.GONE
        binding.cvCall.visibility = View.GONE
        binding.llBottomBtn.visibility = View.GONE
    }

    fun showUI() {

        binding.llMapStyles.visibility = View.VISIBLE
        binding.cvRecover.visibility = View.VISIBLE
        binding.cvNotif.visibility = View.VISIBLE
        binding.cvCall.visibility = View.VISIBLE
        binding.llBottomBtn.visibility = View.VISIBLE
    }
}