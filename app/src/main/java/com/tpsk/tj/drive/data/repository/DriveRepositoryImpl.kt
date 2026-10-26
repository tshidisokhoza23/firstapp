package com.tpsk.tj.drive.data.repository

import androidx.compose.ui.graphics.Color
import com.tpsk.tj.drive.data.api.DriveApi
import com.tpsk.tj.drive.data.api.SaveDestinationRequest
import com.tpsk.tj.drive.data.local.dao.DriveDao
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.drive.domain.repository.DriveRepository
import com.tpsk.tj.drive.model.*
import com.tpsk.tj.drive.data.network.RideWebSocketClient

import kotlinx.coroutines.flow.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class DriveRepositoryImpl @Inject constructor(
    private val driveApi: DriveApi,
    private val driveDao: DriveDao,
    private val rideWebSocketClient: RideWebSocketClient
) : DriveRepository {

    override fun getRideOptions(): Flow<Resource<List<RideOption>>> = flow {
        emit(Resource.Loading())
        
        // 1. Initial Local Data
        val initialLocal = driveDao.getRideOptions().firstOrNull()
        if (initialLocal != null && initialLocal.isNotEmpty()) {
            emit(Resource.Success(initialLocal.map { it.toRideOption() }))
        }

        // 2. Try Fetching from API
        try {
            val res = driveApi.getRideOptions()
            if (res.isSuccessful && res.body() != null) {
                val apiOptions = res.body()!!.ride_options
                if (apiOptions.isNotEmpty()) {
                    driveDao.clearRideOptions()
                    driveDao.insertRideOptions(apiOptions.map { it.toRideOptionEntity() })
                }
            }
        } catch (_: Exception) {}

        // 3. Continuous Observation + Fallback Seeding
        emitAll(driveDao.getRideOptions().map { entities ->
            if (entities.isEmpty()) {
                val defaults = listOf(
                    RideOption("1", "TJ Go", "0.00", arrivalTime = "3 min", description = "Standard, compact rides", iconColor = Color.Green),
                    RideOption("2", "TJ Comfort", "0.00", arrivalTime = "5 min", description = "Newer cars, extra legroom", iconColor = Color(0xFF2196F3)),
                    RideOption("3", "TJ Premium", "0.00", arrivalTime = "4 min", description = "Luxury & top drivers", iconColor = Color(0xFF9C27B0)),
                    RideOption("4", "TJ XL", "0.00", arrivalTime = "7 min", description = "SUVs for groups up to 6", iconColor = Color(0xFFFF9800))
                )
                // Seed asynchronously and don't emit yet, the collect will pick it up
                driveDao.insertRideOptions(defaults.map { it.toRideOptionEntity() })
                Resource.Loading() // Wait for seed to settle
            } else {
                Resource.Success(entities.map { it.toRideOption() })
            }
        })
    }

    override fun getDestinations(): Flow<Resource<List<Destination>>> = flow {
        emit(Resource.Loading())
        driveDao.getDestinations().firstOrNull()?.let { 
            if (it.isNotEmpty()) emit(Resource.Success(it.map { it.toDestination() })) 
        }
        try {
            val res = driveApi.getDestinations()
            if (res.isSuccessful && res.body() != null) {
                driveDao.insertDestinations(res.body()!!.destinations.map { it.toDestinationEntity() })
            }
        } catch (_: Exception) {}
        driveDao.getDestinations().collect { entities ->
            emit(Resource.Success(entities.map { it.toDestination() }))
        }
    }

    override fun getTrips(): Flow<Resource<List<Trip>>> = flow {
        emit(Resource.Loading())
        try {
            val res = driveApi.getTrips()
            if (res.isSuccessful && res.body() != null) {
                emit(Resource.Success(res.body()!!.trips))
            } else {
                emit(Resource.Error("Failed to fetch trips"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun saveDestination(
        name: String,
        address: String,
        latitude: Double,
        longitude: Double,
        isFavorite: Boolean,
        type: String
    ): Flow<Resource<Boolean>> = flow {
        val destination = Destination(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
            iconColor = Color.Gray,
            isFavorite = isFavorite,
            type = try { DestinationType.valueOf(type.uppercase()) } catch (_: Exception) { DestinationType.RECENT }
        )
        driveDao.insertDestinations(listOf(destination.toDestinationEntity()))
        emit(Resource.Success(true))
        try {
            driveApi.saveDestination(SaveDestinationRequest(name, address, latitude, longitude, isFavorite, type))
        } catch (_: Exception) {}
    }

    override fun deleteDestination(id: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        driveDao.deleteDestination(id)
        emit(Resource.Success(true))
    }

    override fun getDriverDocuments(): Flow<Resource<List<DriverDocument>>> = flow {
        emit(Resource.Loading())
        try {
            val res = driveApi.getDriverDocuments()
            if (res.isSuccessful && res.body() != null) {
                emit(Resource.Success(res.body()!!.documents))
            } else {
                emit(Resource.Error("Failed to fetch documents"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun uploadDocument(type: String, url: String): Flow<Resource<DriverDocument>> = flow {
        emit(Resource.Loading())
        try {
            val res = driveApi.uploadDocument(com.tpsk.tj.drive.data.api.UploadDocumentRequest(type, url))
            if (res.isSuccessful && res.body() != null) {
                emit(Resource.Success(res.body()!!.document))
            } else {
                emit(Resource.Error(res.errorBody()?.string()?.ifBlank { null } ?: "Upload failed"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun uploadDocumentPhoto(type: String, file: okhttp3.MultipartBody.Part): Flow<Resource<DriverDocument>> = flow {
        emit(Resource.Loading())
        try {
            val body = type.toRequestBody("text/plain".toMediaType())
            val res = driveApi.uploadDocumentPhoto(body, file)
            if (res.isSuccessful && res.body() != null) {
                emit(Resource.Success(res.body()!!.document))
            } else {
                emit(Resource.Error(apiError(res, "Upload failed")))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun getWalletActivity(): Flow<Resource<WalletActivity>> = flow {
        emit(Resource.Loading())
        try {
            val res = driveApi.getWalletActivity()
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                emit(Resource.Success(WalletActivity(body.transactions, body.availablePayout, body.weekEarnings)))
            } else {
                emit(Resource.Error("Failed to load payments"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun getAvailableRequests(lat: Double, lng: Double): Flow<Resource<List<RideRequest>>> = flow {
        emit(Resource.Loading())
        try {
            val res = driveApi.getAvailableRequests(lat, lng)
            if (res.isSuccessful && res.body() != null) {
                emit(Resource.Success(res.body()!!.requests))
            } else {
                emit(Resource.Error("Failed to fetch requests"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun updateOnlineStatus(online: Boolean, latitude: Double, longitude: Double): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val res = driveApi.updateOnlineStatus(com.tpsk.tj.drive.data.api.UpdateStatusRequest(online, latitude, longitude))
            if (res.isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error(apiError(res, "Could not change online status")))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun createRideRequest(
        pickup: String,
        dest: String,
        pLat: Double,
        pLng: Double,
        dLat: Double,
        dLng: Double,
        fare: Double,
        distance: String,
        type: String,
        meters: Int,
        seconds: Int,
        stops: List<com.tpsk.tj.drive.data.api.RideStopBody>
    ): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val res = driveApi.createRideRequest(com.tpsk.tj.drive.data.api.CreateRideRequest(
                pickup, dest, pLat, pLng, dLat, dLng, fare, distance, type, seconds, meters, stops
            ))
            if (res.isSuccessful && res.body() != null) {
                emit(Resource.Success(res.body()!!.ride_id))
            } else {
                emit(Resource.Error(apiError(res, "Failed to create request")))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun reportPresence(lat: Double, lng: Double): Flow<Resource<AreaPresence>> = flow {
        try {
            val res = driveApi.reportPresence(com.tpsk.tj.drive.data.api.PresenceRequest(lat, lng))
            if (res.isSuccessful && res.body() != null) emit(Resource.Success(res.body()!!))
            else emit(Resource.Error(apiError(res, "Couldn't check who is nearby")))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Couldn't check who is nearby"))
        }
    }

    override fun markStop(rideId: String, index: Int, action: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val res = driveApi.markStop(com.tpsk.tj.drive.data.api.MarkStopRequest(rideId, index, action))
            if (res.isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error(apiError(res, "Could not update the stop")))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Could not update the stop"))
        }
    }

    override fun updateRideStatus(rideId: String, status: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val res = driveApi.updateRideStatus(com.tpsk.tj.drive.data.api.UpdateRideStatusRequest(rideId, status))
            if (res.isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error(apiError(res, "Could not update the ride")))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun cancelRide(rideId: String, reason: String, by: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val res = driveApi.cancelRide(com.tpsk.tj.drive.data.api.CancelRideRequest(rideId, reason, by))
            if (res.isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error(apiError(res, "Could not cancel the ride")))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun getVehicleInfo(): Flow<Resource<VehicleInfo>> = flow {
        emit(Resource.Loading())
        try {
            val res = driveApi.getVehicleInfo()
            if (res.isSuccessful && res.body() != null) {
                emit(Resource.Success(res.body()!!))
            } else {
                emit(Resource.Error("Failed to fetch vehicle info"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun uploadVehiclePhoto(file: okhttp3.MultipartBody.Part): Flow<Resource<VehicleInfo>> = flow {
        emit(Resource.Loading())
        try {
            val res = driveApi.uploadVehiclePhoto(file)
            if (res.isSuccessful && res.body() != null) {
                emit(Resource.Success(res.body()!!))
            } else {
                emit(Resource.Error(apiError(res, "Could not add that car photo")))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun uploadDriverProfilePhoto(file: okhttp3.MultipartBody.Part): Flow<Resource<VehicleInfo>> = flow {
        emit(Resource.Loading())
        try {
            val res = driveApi.uploadDriverProfilePhoto(file)
            if (res.isSuccessful && res.body() != null) {
                emit(Resource.Success(res.body()!!))
            } else {
                emit(Resource.Error(apiError(res, "Could not save that profile picture")))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun removeVehiclePhoto(url: String): Flow<Resource<VehicleInfo>> = flow {
        emit(Resource.Loading())
        try {
            val res = driveApi.removeVehiclePhoto(com.tpsk.tj.drive.data.api.RemoveVehiclePhotoRequest(url))
            if (res.isSuccessful && res.body() != null) {
                emit(Resource.Success(res.body()!!))
            } else {
                emit(Resource.Error(apiError(res, "Could not remove that photo")))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun updateVehicleInfo(vehicle: VehicleInfo): Flow<Resource<VehicleInfo>> = flow {
        emit(Resource.Loading())
        try {
            val res = driveApi.updateVehicleInfo(vehicle)
            if (res.isSuccessful && res.body() != null) {
                emit(Resource.Success(res.body()!!))
            } else {
                emit(Resource.Error("Failed to update vehicle info"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun getNearbyDrivers(lat: Double, lng: Double): Flow<Resource<List<DriverStatus>>> = flow {
        emit(Resource.Loading())
        try {
            val res = driveApi.getNearbyDrivers(lat, lng)
            if (res.isSuccessful && res.body() != null) {
                emit(Resource.Success(res.body()!!.drivers))
            } else {
                emit(Resource.Error("Failed to fetch nearby drivers"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun getRideEvents() = rideWebSocketClient.events

    override fun connectRideSocket() {
        rideWebSocketClient.connect()
    }

    override fun disconnectRideSocket() {
        rideWebSocketClient.disconnect()
    }

    override fun getRideState(): Flow<Resource<RideLiveState>> = flow {
        try {
            val res = driveApi.getRideState()
            if (res.isSuccessful && res.body() != null) emit(Resource.Success(res.body()!!))
            else emit(Resource.Error(apiError(res, "Could not load the ride")))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun getDriverPlan(): Flow<Resource<DriverFeePlan>> = flow {
        try {
            val res = driveApi.getDriverPlan()
            if (res.isSuccessful && res.body() != null) emit(Resource.Success(res.body()!!))
            else emit(Resource.Error(apiError(res, "Could not load driver plan")))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun setDriverPlan(plan: String): Flow<Resource<DriverFeePlan>> = flow {
        try {
            val res = driveApi.setDriverPlan(com.tpsk.tj.drive.data.api.SetDriverPlanRequest(plan))
            if (res.isSuccessful && res.body() != null) emit(Resource.Success(res.body()!!))
            else emit(Resource.Error(apiError(res, "Could not save the plan")))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun topUpWallet(amount: Double): Flow<Resource<com.tpsk.tj.drive.data.api.TopUpResponse>> = flow {
        try {
            val res = driveApi.topUpWallet(com.tpsk.tj.drive.data.api.TopUpRequest(amount))
            if (res.isSuccessful && res.body() != null) emit(Resource.Success(res.body()!!))
            else emit(Resource.Error(apiError(res, "Could not start card payment")))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun confirmTopUp(reference: String): Flow<Resource<Double>> = flow {
        try {
            val res = driveApi.confirmTopUp(com.tpsk.tj.drive.data.api.ConfirmTopUpRequest(reference))
            if (res.isSuccessful && res.body() != null) emit(Resource.Success(res.body()!!.wallet_balance))
            else emit(Resource.Error(apiError(res, "Payment is not finished yet")))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun redeemVoucher(pin: String): Flow<Resource<Double>> = flow {
        try {
            val res = driveApi.redeemVoucher(com.tpsk.tj.drive.data.api.VoucherRequest(pin))
            if (res.isSuccessful && res.body() != null) emit(Resource.Success(res.body()!!.wallet_balance))
            else emit(Resource.Error(apiError(res, "Could not redeem voucher")))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun startRide(rideId: String, pin: String): Flow<Resource<Boolean>> = flow {
        try {
            val res = driveApi.startRide(com.tpsk.tj.drive.data.api.StartRideRequest(rideId, pin))
            if (res.isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error(apiError(res, "PIN is wrong")))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    private fun apiError(res: retrofit2.Response<*>, fallback: String): String {
        val raw = res.errorBody()?.string().orEmpty()
        val parsed = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.getOrNull(1)
        return parsed ?: fallback
    }

    override fun getPayoutAccount(): Flow<Resource<com.tpsk.tj.drive.data.api.DriverPayoutAccount>> = flow {
        emit(Resource.Loading())
        try {
            val res = driveApi.getPayoutAccount()
            if (res.isSuccessful && res.body() != null) emit(Resource.Success(res.body()!!))
            else emit(Resource.Error(apiError(res, "Could not load payout account")))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun savePayoutAccount(name: String, number: String, bankCode: String, bankName: String): Flow<Resource<com.tpsk.tj.drive.data.api.DriverPayoutAccount>> = flow {
        emit(Resource.Loading())
        try {
            val res = driveApi.savePayoutAccount(com.tpsk.tj.drive.data.api.SavePayoutAccountRequest(name, number, bankCode, bankName))
            if (res.isSuccessful && res.body() != null) emit(Resource.Success(res.body()!!))
            else emit(Resource.Error(apiError(res, "Could not save bank account")))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun getPayoutBanks(): Flow<Resource<List<com.tpsk.tj.drive.data.api.PayoutBank>>> = flow {
        emit(Resource.Loading())
        try {
            val res = driveApi.getPayoutBanks()
            if (res.isSuccessful && res.body() != null) emit(Resource.Success(res.body()!!.banks))
            else emit(Resource.Error(apiError(res, "Could not load banks")))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun getActiveSession(): Flow<Resource<ActiveSession>> = flow {
        emit(Resource.Loading())
        try {
            val res = driveApi.getActiveSession()
            if (res.isSuccessful && res.body() != null) {
                emit(Resource.Success(res.body()!!))
            } else {
                emit(Resource.Error("No active session"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }
}
