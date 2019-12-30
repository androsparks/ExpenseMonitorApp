package com.expensemoitor.expensemonitor.network

import com.expensemoitor.expensemonitor.utilites.AppConstants

object ApiFactory {

    val CREATE_EXPENSE_SERVICE:CreateExpenseService = RetrofitFactory.retrofit(AppConstants.BASEURL)
        .create(CreateExpenseService::class.java)


    val REGISTERATION_SERVICE:RegisterationService = RetrofitFactory.retrofit((AppConstants.BASEURL))
        .create(RegisterationService::class.java)


    val GET_DURATION_EXPNSES_SERVICE:GetTodayExpenseService = RetrofitFactory.retrofit((AppConstants.BASEURL))
        .create(GetTodayExpenseService::class.java)


    val DELETE_EXPENSE:DeleteExpenseService = RetrofitFactory.retrofit(AppConstants.BASEURL)
        .create(DeleteExpenseService::class.java)

    val UPDATE_EXPENSE:UpdateExpenseService = RetrofitFactory.retrofit(AppConstants.BASEURL)
        .create(UpdateExpenseService::class.java)

}