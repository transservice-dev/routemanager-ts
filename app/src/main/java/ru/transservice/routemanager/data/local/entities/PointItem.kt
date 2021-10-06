package ru.transservice.routemanager.data.local.entities

import androidx.core.text.isDigitsOnly
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import org.jetbrains.annotations.NotNull
import ru.transservice.routemanager.data.remote.res.task.TaskUploadBody
import ru.transservice.routemanager.extensions.longFormat
import java.io.Serializable
import java.util.*
import kotlin.math.max

@Entity(tableName = "pointList_table",primaryKeys = ["docUID","lineUID"], indices = [Index("docUID","lineUID","docUID","lineUID")])
data class PointItem(
    val docUID: String,
    val lineUID: String,
    val rowNumber: Int,
    val addressName: String,
    val addressLon: Double,
    val addressLat: Double,
    val containerName: String,
    val containerSize: Double,
    val agentName: String,
    val countPlan: Double,
    var countFact: Double,
    var countOver: Double,
    var done: Boolean,
    val tripNumber: Int,
    val polygon: Boolean,
    val routeName: String,
    val comment: String,
    val noPhotoAllowed: Boolean = false,
    val noEditFact: Boolean = false,
    var reasonComment: String = "",
    @ColumnInfo(defaultValue = "")
    val polygonUID: String,
    @ColumnInfo(defaultValue = "")
    val polygonName: String,
    @ColumnInfo(defaultValue = "0")
    val polygonByRow: Boolean,
    @ColumnInfo(defaultValue = "1000")
    var tripNumberFact: Int
) : Serializable {
    var timestamp: Date? = null
    var status : PointStatuses = PointStatuses.NOT_VISITED

    @Ignore
    var pointActionsArray: ArrayList<PointActions> = ArrayList()
    @Ignore
    var pointActionsCancelArray : ArrayList<PointActions> = ArrayList()

    init {
        pointActionsArray.add(PointActions.TAKE_PHOTO_AFTER)
        pointActionsArray.add(PointActions.TAKE_PHOTO_BEFORE)
        pointActionsArray.add(PointActions.SET_VOLUME)

        pointActionsCancelArray.add(PointActions.TAKE_PHOTO_BEFORE)
        pointActionsCancelArray.add(PointActions.SET_REASON)
    }

    // constructor for polygon point
    constructor (
        docUID: String,
        lineUID: String,
        addressName: String,
        tripNumber: Int,
        polygon: Boolean,
        polygonUID: String
    ) : this(
        docUID,
        lineUID,
        5000,
        addressName,
        0.0,
        0.0,
        "",
        0.0,
        "",
        0.0,
        0.0,
        0.0,
        false,
        tripNumber,
        polygon,
        "",
        "",
        false,
        false,
        "",
        polygonUID,
        addressName,
        false,
        tripNumber
    )

    fun setCountOverFromPlanAndFact(){
        this.countOver = max(0.0, this.countFact - this.countPlan)
    }

    //get phone number from comment
    fun getPhoneFromComment () : String{
        var phoneNumber = ""
        for (char in this.comment){
            if(char.isDigit() || (char == '+' && phoneNumber == "")) {
                phoneNumber += char
                if (phoneNumber.isDigitsOnly() && phoneNumber.length >= 11){
                    return phoneNumber
                } else if (phoneNumber.length >= 12){
                    return phoneNumber
                }
            }else if (char == '(' || char == ')'
                    || char == '-' || char == ' '){
                continue
            } else {
                phoneNumber = ""
            }
        }

        return if (phoneNumber.isDigitsOnly() && phoneNumber.length >= 11){
            phoneNumber
        } else if (phoneNumber.length >= 12){
            phoneNumber
        } else {
            ""
        }

    }

    fun isPolygonEmpty(): Boolean {
       return polygonUID.isNullOrEmpty() || polygonUID == "00000000-0000-0000-0000-000000000000"
    }

    fun polygonNotFilled(): Boolean {
        return this.polygonByRow && this.isPolygonEmpty()
    }

    fun toTaskUploadBody(): TaskUploadBody {
        //TODO Подумать как реализовать это дело лучше, найти проблему, почему переменные принимают тип INFINITY
        return TaskUploadBody(
            this.docUID,
            this.lineUID,
            if (this.countFact.isFinite()) this.countFact else 0.0, //check if countFact and countOver is finite
            if (this.countOver.isFinite()) this.countOver else 0.0,
            this.done,
            this.reasonComment,
            this.timestamp?.longFormat() ?: "",
            polygonUID,
            polygonName,
            polygon,
            tripNumberFact
        )
    }
}

/*@DatabaseView("SELECT lineUID, rowNumber, addressName, containerName, containerSize, agentName, " +
                            "countPlan, countFact, countOver, done, tripNumber, polygon, " +
                            "comment, reasonComment, timestamp " +
                    "FROM pointList_table as points ")
data class PointItem(
        val lineUID: String,
        val rowNumber: Int,
        val addressName: String,
        val containerName: String,
        val containerSize: Double,
        val agentName: String,
        val countPlan: Double,
        var countFact: Double,
        var countOver: Double,
        var done: Boolean,
        val tripNumber: Int,
        val polygon: Boolean,
        val comment: String
): Serializable {
    var timestamp: Date? = null
    var reasonComment: String = ""
}
*/
enum class PointActions : Serializable {
    TAKE_PHOTO_BEFORE, TAKE_PHOTO_AFTER, SET_VOLUME, SET_REASON
}

enum class PointStatuses : Serializable {
    NOT_VISITED,CANNOT_DONE,DONE
}

enum class FailureReasons(val reasonTitle: String){
    NO_GARBAGE("нет ТКО"),
    CARS_ON_POINT("нет проезда к КП (заставлено автомашинами)"),
    ROAD_REPAIR("нет проезда (ремонт дороги)"),
    DOORS_CLOSED("не открывают ворота (шлагбаум)"),
    CLIENT_DENIAL("отказ Потребителя от вывоза ТКО"),
    NO_EQUIPMENT("нет контейнерного оборудования"),
    EQUIPMENT_LOCKED("контейнер(а) на замке"),
    WEATHER_CONDITIONS("погодные условия"),
    OTHER("другое")
}



