package com.codeco.tabacaleros.features.DNI.data

import com.codeco.tabacaleros.features.DNI.domain.DNIRepository
import com.codeco.tabacaleros.features.DNI.model.ProductorInfo
import com.codeco.tabacaleros.servicios.SessionManager
import com.codeco.tabacaleros.servicios.internetCommunication.ActividadesDesarrolloService
import com.codeco.tabacaleros.servicios.internetCommunication.DatosContactoService
import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadesDesarrolloRequest
import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadesDesarrolloResponse

class DNIRepositoryImpl(
    private val datosContactoService: DatosContactoService,
    private val actividadesDesarrolloService: ActividadesDesarrolloService,
    private val sessionManager: SessionManager
) : DNIRepository {

    override suspend fun getProductorInfo(cuil: String): Result<ProductorInfo> {
        return try {
            val response = datosContactoService.getDatosContacto(cuil)
            if (response.success) {
                Result.success(
                    ProductorInfo(
                        idProductor = response.idProductor,
                        cuil = response.cuil,
                        nombre = response.nombre,
                        fechaNacimiento = response.fechaNacimiento,
                        sexo = response.sexo,
                        estado = response.estado,
                        urlDni = response.urlDni,
                        urlFoto = response.urlFoto,
                        productorPhone = null,
                        phoneVerification = null
                    )
                )
            } else {
                Result.failure(Exception("CUIL no encontrado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveProductorInfo(productorInfo: ProductorInfo) {
        sessionManager.saveProducerInfo(productorInfo)
    }

    override suspend fun getActividades(idProductor: Int): Result<ActividadesDesarrolloResponse> {
        return try {
            val request = ActividadesDesarrolloRequest(idProductor = idProductor, idCategoria = 9000)
            val response = actividadesDesarrolloService.getActividadesDesarrollo(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error fetching activities: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
