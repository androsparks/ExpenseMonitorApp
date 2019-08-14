package com.expensemoitor.expensemonitor.createnewexpense


import android.app.Application
import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.expensemoitor.expensemonitor.utilites.displayCurrentDate
import android.widget.AdapterView
import android.widget.Spinner
import com.expensemoitor.expensemonitor.R
import com.expensemoitor.expensemonitor.network.ApiFactory
import com.expensemoitor.expensemonitor.network.ExpenseData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class CreateNewExpenseFragmentViewModel(var application: Application) : ViewModel() {



    val amount = MutableLiveData<String>()
    val description = MutableLiveData<String>()
    val currentDate = MutableLiveData<String>()
    var selectedFormsItem = ""
    var selectedCategoryItem = ""



    init {
        currentDate.value = displayCurrentDate()
    }
    private val _validationMsg = MutableLiveData<String>()
    val validationMsg: LiveData<String>
        get() = _validationMsg

    private val _navigateToMyExpenseFragment = MutableLiveData<Boolean>()
    val navigateToMyExpenseFragment : LiveData<Boolean>
        get() = _navigateToMyExpenseFragment


    fun onSelectExpenseFormOrCategoryItem(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        val forms = parent as Spinner
        if(forms.id == R.id.forms_spinner){
            selectedFormsItem = parent.selectedItem.toString()
        }else if(parent.id == R.id.categories_spinner){
            selectedCategoryItem = parent.selectedItem.toString()
        }
    }



    fun createExpenseClick(){
        val expenseAmount = amount.value
        val expenseDescription = description.value

        if(expenseAmount == null || expenseDescription == null){
            _validationMsg.value = "Please fill empty field"
        }else if (selectedFormsItem.equals(application.getString(R.string.SelectForms))){
            _validationMsg.value = "Please select Expense Forms"
        }else if(selectedCategoryItem.equals(application.getString(R.string.SelectCategory))){
            _validationMsg.value = "Please select Expense Category"
        }else{
            currentDate.value?.let {
                createNewExpense(expenseAmount,expenseDescription,
                    it,selectedFormsItem,selectedCategoryItem)
            }
        }
    }


    private var viewModelJob = Job()
    private val corotuineJob  = CoroutineScope(viewModelJob + Dispatchers.Main)



    private fun createNewExpense(amount:String,description:String,date:String,form:String,category:String){
           corotuineJob.launch {
            val expenseData = ExpenseData(amount,description,date,form,category)
            val getResponse = ApiFactory.expenseUrls.createNewExpense(expenseData)
            try {
              val expensResponse = getResponse.await()
                Log.d("response",expensResponse.message.toString())
                _navigateToMyExpenseFragment.value = true
            }catch (t:Throwable){
                _navigateToMyExpenseFragment.value = false
            }
           }
    }




    fun onNoEmptyFields(){
        _validationMsg.value = ""
    }



    fun onNavigateToMyExpnse(){
        _navigateToMyExpenseFragment.value = false
    }



    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }


}