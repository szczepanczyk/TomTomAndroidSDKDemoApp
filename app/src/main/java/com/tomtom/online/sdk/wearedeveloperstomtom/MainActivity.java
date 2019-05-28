package com.tomtom.online.sdk.wearedeveloperstomtom;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.tomtom.online.sdk.common.location.LatLng;
import com.tomtom.online.sdk.map.Icon;
import com.tomtom.online.sdk.map.MapFragment;
import com.tomtom.online.sdk.map.Marker;
import com.tomtom.online.sdk.map.MarkerBuilder;
import com.tomtom.online.sdk.map.OnMapReadyCallback;
import com.tomtom.online.sdk.map.RouteBuilder;
import com.tomtom.online.sdk.map.SimpleMarkerBalloon;
import com.tomtom.online.sdk.map.TomtomMap;
import com.tomtom.online.sdk.map.TomtomMapCallback;
import com.tomtom.online.sdk.routing.OnlineRoutingApi;
import com.tomtom.online.sdk.routing.RoutingApi;
import com.tomtom.online.sdk.routing.data.FullRoute;
import com.tomtom.online.sdk.routing.data.RouteQuery;
import com.tomtom.online.sdk.routing.data.RouteQueryBuilder;
import com.tomtom.online.sdk.routing.data.RouteResponse;
import com.tomtom.online.sdk.search.OnlineSearchApi;
import com.tomtom.online.sdk.search.SearchApi;
import com.tomtom.online.sdk.search.data.fuzzy.FuzzySearchQuery;
import com.tomtom.online.sdk.search.data.fuzzy.FuzzySearchQueryBuilder;
import com.tomtom.online.sdk.search.data.fuzzy.FuzzySearchResponse;
import com.tomtom.online.sdk.search.data.fuzzy.FuzzySearchResult;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback, TomtomMapCallback.OnMarkerClickListener, TomtomMapCallback.OnMapLongClickListener {

    private TomtomMap tomtomMap;

    private Marker departureMarker;
    private Marker destinationMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        mapFragment.getAsyncMap(this);
    }

    @Override
    public void onMapReady(@NonNull TomtomMap tomtomMap) {
        this.tomtomMap = tomtomMap;
        this.tomtomMap.addOnMarkerClickListener(this);
        this.tomtomMap.addOnMapLongClickListener(this);

        SearchApi searchApi = OnlineSearchApi.create(this);

        FuzzySearchQuery searchQuery = FuzzySearchQueryBuilder.create("Gym 14055 Berlin Germany")
                .build();

        searchApi.search(searchQuery)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSingleObserver<FuzzySearchResponse>() {
                    @Override
                    public void onSuccess(FuzzySearchResponse fuzzySearchResponse) {

                        for (FuzzySearchResult result : fuzzySearchResponse.getResults()) {
                            tomtomMap.addMarker(
                                    new MarkerBuilder(result.getPosition()).markerBalloon(
                                            new SimpleMarkerBalloon(result.getPoi().getName())));
                        }
                        tomtomMap.zoomToAllMarkers();
                    }

                    @Override
                    public void onError(Throwable e) {
                    }
                });
    }

    @Override
    public void onMarkerClick(@NonNull Marker marker) {
        if (departureMarker == null) {
            departureMarker = marker;
        } else if (destinationMarker == null) {
            destinationMarker = marker;
            RoutingApi routingApi =
                    OnlineRoutingApi.create(this);
            RouteQuery routeQuery = RouteQueryBuilder.create(
                    departureMarker.getPosition(),
                    destinationMarker.getPosition()).build();
            routingApi.planRoute(routeQuery)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new DisposableSingleObserver<RouteResponse>() {
                        @Override
                        public void onSuccess(RouteResponse routeResponse) {
                            if (routeResponse.hasResults()) {
                                FullRoute fullRoute = routeResponse.getRoutes().get(0);
                                tomtomMap.addRoute(new RouteBuilder(fullRoute.getCoordinates())
                                        .startIcon(Icon.Factory.fromResources(MainActivity.this,
                                                R.drawable.ic_map_route_departure))
                                        .endIcon(Icon.Factory.fromResources(MainActivity.this,
                                                R.drawable.ic_map_route_destination)));
                            }
                            tomtomMap.displayRoutesOverview();
                        }

                        @Override
                        public void onError(Throwable e) {
                        }
                    });
        }
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        departureMarker = null;
        destinationMarker = null;
        tomtomMap.clearRoute();
    }
}
