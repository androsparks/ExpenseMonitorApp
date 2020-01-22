package com.monitoryourexpenses.expenses.registeruser


import android.app.Application
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.monitoryourexpenses.expenses.network.UserData
import com.monitoryourexpenses.expenses.R
import com.monitoryourexpenses.expenses.database.ExpenseMonitorDao
import com.monitoryourexpenses.expenses.database.UserExpenses
import com.monitoryourexpenses.expenses.network.ApiFactory
import com.monitoryourexpenses.expenses.utilites.*
import com.monitoryourexpenses.expenses.utilites.Converter.Companion.toBigDecimal
import com.monitoryourexpenses.expenses.utilites.MyApp.Companion.context
import kotlinx.coroutines.*
import org.threeten.bp.LocalDate
import retrofit2.HttpException


class RegisterationUserViewModel(val database: ExpenseMonitorDao, var application: Application) :ViewModel() {


    var radiochecked = MutableLiveData<Int>()
    private var geneder = ""
    var currency = ""


    init {
        //save dates for the first time so it can be updated later
        saveAllDates()
    }


    private val _status = MutableLiveData<ProgressStatus>()
    val status: LiveData<ProgressStatus>
        get() = _status

    private val _navigateToNextScreen = MutableLiveData<Boolean>()
    val navigateToNextScreen: LiveData<Boolean>
        get() = _navigateToNextScreen


    private val _genderSelected = MutableLiveData<Boolean>()
    val genderSelected: LiveData<Boolean>
        get() = _genderSelected


    private val _errormsg = MutableLiveData<String>()
    val errormsg : LiveData<String>
        get() = _errormsg


    fun onSelectCurrencyItem(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
            currency = parent.selectedItem.toString()
    }





    fun registerUser(userName:String,emailAddress:String) {



        when(radiochecked.value){
            R.id.male_radio_button->{
                geneder = "male"
            }

            R.id.female_radio_button->{
                geneder ="female"
            }
        }

        if (geneder.isEmpty()){
            _genderSelected.value = false
        }else if(currency == context?.getString(R.string.select_currency)){
            _genderSelected.value = false
        }else{
            val userData = UserData(userName,emailAddress,geneder,currency)
            viewModelScope.launch {
                val getUserResponse =  ApiFactory.REGISTERATION_SERVICE.registerationUserAsync(userData)
                try {
                    try {
                        _status.value = ProgressStatus.LOADING
                        val userResponse = getUserResponse.await()
                        PrefManager.saveCurrency(application,currency.substring(range = 0..2))
                        saveCurrencyForSettings(currency)
                        PrefManager.setUserRegistered(application,true)
                        PrefManager.saveAccessToken(application,userResponse.accessToken)
                        database.insertExpense(UserExpenses(
                            todayExpenses = toBigDecimal("0"),
                            weekExpenses  = toBigDecimal("0"),
                            monthExpenses = toBigDecimal("0"),
                            currency      = currency
                        ))
                        _navigateToNextScreen.value = true
                        _status.value = ProgressStatus.DONE
                    }catch (t:Throwable){
                        Log.d("throwable",t.toString())
                        _status.value = ProgressStatus.ERROR
                        _errormsg.value = context?.getString(R.string.weak_internet_connection)
                    }
                }catch (httpException:HttpException){
                    Log.d("httpException",httpException.message())
                }
            }
        }
        }


    private fun saveAllDates(){
        viewModelScope.launch {
            PrefManager.saveCurrentDate(application,LocalDate.now().toString())
            PrefManager.saveStartOfTheWeek(application,LocalDate.now().toString())
            PrefManager.saveStartOfTheMonth(application,LocalDate.now().toString())
            PrefManager.saveEndOfTheWeek(application,LocalDate.now().plusDays(7).toString())
            PrefManager.saveEndOfTheMonth(application,LocalDate.now().plusMonths(1).toString())
        }
    }

    fun genderAlreadySelected(){
        _genderSelected.value = true
    }

    fun onErrorMsgDisplayed(){
        _errormsg.value = null
    }

    fun onNavigationCompleted(){
        _navigateToNextScreen.value = false
    }



}