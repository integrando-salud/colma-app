package com.codeco.tabacaleros.servicios.internetCommunication.model

import com.google.gson.annotations.SerializedName

data class ActividadDesarrolloItem(
    @SerializedName("id") val id: Int,
    @SerializedName("id_contacto_empresa") val idContactoEmpresa: Int,
    @SerializedName("id_contacto_productor") val idContactoProductor: Int,
    @SerializedName("id_tabaco_actividad") val idTabacoActividad: Int,
    @SerializedName("cupo_plantas") val cupoPlantas: Int,
    @SerializedName("sn_activo") val snActivo: Int,
    @SerializedName("sn_cupo_activo") val snCupoActivo: Int?,
    @SerializedName("sn_censado") val snCensado: Int,
    @SerializedName("id_censa_empresa") val idCensaEmpresa: Int?,
    @SerializedName("id_censa_en_radicacion") val idCensaEnRadicacion: Int?,
    @SerializedName("telefono_censo_productor") val telefonoCensoProductor: String?,
    @SerializedName("sn_censo_productor_telefono_verificado") val snCensoProductorTelefonoVerificado: Int,
    @SerializedName("censo_id_user_censista") val censoIdUserCensista: Int?,
    @SerializedName("censo_link_dni_productor") val censoLinkDniProductor: String?,
    @SerializedName("censo_link_foto_productor") val censoLinkFotoProductor: String?,
    @SerializedName("censo_plantas_productor") val censoPlantasProductor: Int,
    @SerializedName("censo_cuadros_cantidad") val censoCuadrosCantidad: Int,
    @SerializedName("censo_kilos_estimado_productor") val censoKilosEstimadoProductor: Int,
    @SerializedName("censo_fecha_registro_app") val censoFechaRegistroApp: String?,
    @SerializedName("censo_fecha_recibido_central") val censoFechaRecibidoCentral: String?,
    @SerializedName("empresa") val empresa: Empresa,
    @SerializedName("tabaco_actividad") val tabacoActividad: TabacoActividad
)
