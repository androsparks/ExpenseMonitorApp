package com.monitoryourexpenses.expenses.myexpenses

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.EntryXComparator
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.monitoryourexpenses.expenses.R
import com.monitoryourexpenses.expenses.adapters.MONTH_EXPENSE_INDEX
import com.monitoryourexpenses.expenses.adapters.PagerAdapter
import com.monitoryourexpenses.expenses.adapters.TODAY_EXPENSE_INDEX
import com.monitoryourexpenses.expenses.adapters.WEEK_EXPENSE_INDEX
import com.monitoryourexpenses.expenses.database.ExpenseMonitorDataBase
import com.monitoryourexpenses.expenses.databinding.MyExpenseFragmentBinding
import com.monitoryourexpenses.expenses.utilites.PrefManager
import com.monitoryourexpenses.expenses.utilites.isConnected
import com.monitoryourexpenses.expenses.utilites.toast
import java.util.*
import kotlin.collections.ArrayList
import kotlinx.android.synthetic.main.barchart_bottom_sheets.view.*
import kotlinx.android.synthetic.main.piechart_bottom_sheets.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.threeten.bp.LocalDate

class MyExpenseFragment : Fragment() {

    private lateinit var binding: MyExpenseFragmentBinding
    @ExperimentalCoroutinesApi
    lateinit var viewModel: MyExpenseFragmentViewModel
    private var mGoogleSignInClient: GoogleSignInClient? = null

    private val appUpdateManager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(this.requireContext()) }
    private val appUpdatedListener: InstallStateUpdatedListener by lazy {
        object : InstallStateUpdatedListener {
            override fun onStateUpdate(installState: InstallState) {
                when {
                    installState.installStatus() == InstallStatus.DOWNLOADED -> popupSnackbarForCompleteUpdate()
                    installState.installStatus() == InstallStatus.INSTALLED -> appUpdateManager.unregisterListener(this)
                    else -> Log.d("Install",
                        installState.installStatus().toString()
                    )
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        if (context?.let { PrefManager.hasCurrency(it) == false }!!) {
            findNavController().navigate(R.id.action_myExpenseFragment_to_userCurrencyFragment)
        }

        binding = DataBindingUtil.inflate(inflater, R.layout.my_expense_fragment, container, false)

        val application = requireNotNull(this.activity).application
        val dataBase = ExpenseMonitorDataBase.getInstance(application).expenseMonitorDao
        val viewModelFactory = MyExpenseFragmentViewModelFactory(dataBase, application)

        viewModel = ViewModelProvider(this, viewModelFactory)
            .get(MyExpenseFragmentViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        (activity as AppCompatActivity).setSupportActionBar(binding.bottomAppBar)
        setHasOptionsMenu(true)

        if (isConnected()) { checkForAppUpdate() }

        val gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
        mGoogleSignInClient = context?.let { GoogleSignIn.getClient(it, gso) }

        val tabLayout = binding.tabs
        val viewPager = binding.viewPager

        viewPager.adapter = PagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getTabTitle(position)
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            @SuppressLint("SetTextI18n")
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> {
                        viewModel.todayExpense.observe(viewLifecycleOwner, Observer {
                        binding.expenseTextView.text = PrefManager.getCurrency(context) + " " + it
                        })
                        binding.dateTextView.text = LocalDate.now().toString()
                    }
                    1 -> {
                        viewModel.weekExpense.observe(viewLifecycleOwner, Observer {
                        binding.expenseTextView.text = PrefManager.getCurrency(context) + " " + it
                        })
                        binding.dateTextView.text = PrefManager.getStartOfTheWeek(context) + " " + "/" + " " + PrefManager.getEndOfTheWeek(context)
                    }
                    2 -> {
                        viewModel.monthExpense.observe(viewLifecycleOwner, Observer {
                        binding.expenseTextView.text = PrefManager.getCurrency(context) + " " + it
                        })
                        binding.dateTextView.text = PrefManager.getStartOfTheMonth(context) + " " + "/" + " " + PrefManager.getEndOfTheMonth(context)
                    }
                }
            }
        })

        viewModel.navigateToMyExpense.observe(viewLifecycleOwner, Observer {
            shouldNavigate -> if (shouldNavigate) {
            val navController = binding.root.findNavController()
            MyExpenseFragment.apply {
                exitTransition = MaterialElevationScale(false).apply {
                    duration = resources.getInteger(R.integer.expense_motion_duration_large).toLong()
                }
                reenterTransition = MaterialElevationScale(true).apply {
                    duration = resources.getInteger(R.integer.expense_motion_duration_large).toLong()
                }
            }
            navController.navigate(R.id.action_myExpenseFragment_to_createNewExpenseFragment)
            viewModel.onNavigatedToMyExpense()
            }
        })

        return binding.root
    }

     private fun checkForAppUpdate() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
         appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                try {
                    val installType = when {
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                        else -> null
                    }
                    if (installType == AppUpdateType.IMMEDIATE) appUpdateManager.registerListener(appUpdatedListener)
                    installType?.let {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            it,
                            (context as Activity?)!!,
                            APP_UPDATE_REQUEST_CODE)
                    }
                } catch (e: IntentSender.SendIntentException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_UPDATE_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(context,
                    getString(R.string.app_failed_to_update),
                    Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun popupSnackbarForCompleteUpdate() {
        val snackbar = Snackbar.make(binding.coordinatorlayout,
            getString(R.string.update_download),
            Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction(getString(R.string.restart)) { appUpdateManager.completeUpdate() }
        context?.let { ContextCompat.getColor(it, R.color.color_on_surface_emphasis_high) }?.let {
            snackbar.setActionTextColor(
                it
            )
        }
        snackbar.show()
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    popupSnackbarForCompleteUpdate()
                }
                try {
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            (context as Activity?)!!,
                            APP_UPDATE_REQUEST_CODE)
                    }
                } catch (e: IntentSender.SendIntentException) {
                    e.printStackTrace()
                }
            }
    }

    companion object {
        private const val APP_UPDATE_REQUEST_CODE = 1991
    }

    private fun getTabTitle(position: Int): String? {
        return when (position) {
            TODAY_EXPENSE_INDEX -> getString(R.string.today)
            WEEK_EXPENSE_INDEX -> getString(R.string.week)
            MONTH_EXPENSE_INDEX -> getString(R.string.month)
            else -> null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
    }

   @ExperimentalCoroutinesApi
   override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
         R.id.menu_dark_mode -> {
                val mode = if ((resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_NO) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                }
                AppCompatDelegate.setDefaultNightMode(mode)
            }
         R.id.share_application -> {
                shareApp()
            }
         R.id.action_setting -> {
                val action = MyExpenseFragmentDirections.actionMyExpenseFragmentToSettingsFragment()
                findNavController().navigate(action)
         }
        R.id.report_currency -> {
           currenciesReportsDialog()
        }
        R.id.report_category -> {
            categoriesReportsDialog()
        }
    }
    return true
  }

    private fun shareApp() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "https://play.google.com/store/apps/details?id=com.monitoryourexpenses.expenses"
            )
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    @ExperimentalCoroutinesApi
    private fun categoriesReportsDialog() {
        viewModel.sumationOfCategories?.observe(viewLifecycleOwner, Observer { catogoriesAndAmount ->
            if (catogoriesAndAmount.isNotEmpty()) {
                val dialogBinding = DataBindingUtil
                    .inflate<ViewDataBinding>(LayoutInflater.from(context), R.layout.piechart_bottom_sheets, null, false)
                val dialog = context?.let { BottomSheetDialog(it) }
                dialog?.setContentView(dialogBinding.root.rootView)
                val entries: MutableList<PieEntry> = ArrayList()
                Collections.sort(entries, EntryXComparator())
                catogoriesAndAmount.forEach { i ->
                    entries.add(PieEntry(i.amount.toFloat(), i.expense_category))
                }
                val pieDataSet = PieDataSet(entries, null)
                pieDataSet.setColors(*ColorTemplate.COLORFUL_COLORS)
                pieDataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                pieDataSet.valueLinePart1OffsetPercentage = 15f
                pieDataSet.valueLinePart1Length = 0.1f
                pieDataSet.valueLinePart2Length = 0.1f
                pieDataSet.valueTextColor = Color.BLACK
                pieDataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                val pieData = PieData(pieDataSet)
                pieData.setValueTextSize(14f)
                pieData.setValueTextColor(Color.BLACK)
                dialogBinding.root.rootView.pie_chart.setEntryLabelColor(Color.BLACK)
                dialogBinding.root.rootView.pie_chart.isDrawHoleEnabled = false
                dialogBinding.root.rootView.pie_chart.transparentCircleRadius = 5f
                dialogBinding.root.rootView.pie_chart.holeRadius = 5f
                dialogBinding.root.rootView.pie_chart.setCenterTextSize(5f)
                dialogBinding.root.rootView.pie_chart.setDrawCenterText(true)
                dialogBinding.root.rootView.pie_chart.description.isEnabled = false
                dialogBinding.root.rootView.pie_chart.legend.formSize = 16f
                dialogBinding.root.rootView.pie_chart.legend.textColor = Color.BLACK
                dialogBinding.root.rootView.pie_chart.legend.textSize = 16f
                dialogBinding.root.rootView.pie_chart.legend.form = Legend.LegendForm.CIRCLE
                dialogBinding.root.rootView.pie_chart.legend.xEntrySpace = 3f
                dialogBinding.root.rootView.pie_chart.legend.yEntrySpace = 3f
                dialogBinding.root.rootView.pie_chart.legend.isWordWrapEnabled = true
                dialogBinding.root.rootView.pie_chart.data = pieData
                dialogBinding.root.rootView.pie_chart.invalidate()
                dialog?.show()
            } else {
                context?.toast(getString(R.string.no_data_available))
            }
        })
    }

    @ExperimentalCoroutinesApi
    private fun currenciesReportsDialog() {
        viewModel.sumationOfCurrencies.observe(viewLifecycleOwner, Observer { currenciesAndAmount ->
            if (currenciesAndAmount.isNotEmpty()) {
                val dialogBinding = DataBindingUtil
                    .inflate<ViewDataBinding>(LayoutInflater.from(context), R.layout.barchart_bottom_sheets, null, false)
                val dialog = context?.let { BottomSheetDialog(it) }
                dialog?.setContentView(dialogBinding.root.rootView)
                val barEntries: MutableList<BarEntry> = ArrayList()
                val currenciesList: ArrayList<String> = ArrayList()
                for (i in currenciesAndAmount.indices) {
                    barEntries.add(BarEntry(i.toFloat(), currenciesAndAmount[i].amount.toFloat()))
                    currenciesList.add(currenciesAndAmount[i].currency)
                }
                val barDataSet = BarDataSet(barEntries, "Currencies")
                barDataSet.setColors(*ColorTemplate.COLORFUL_COLORS)
                barDataSet.setDrawValues(true)
                val dataSets: ArrayList<IBarDataSet> = ArrayList()
                dataSets.add(barDataSet)
                val barData = BarData(dataSets)
                dialogBinding.root.rootView.bar_chart.data = barData
                dialogBinding.root.rootView.bar_chart.xAxis.granularity = 1f
                dialogBinding.root.rootView.bar_chart.xAxis.isGranularityEnabled = true
                dialogBinding.root.rootView.bar_chart.xAxis.valueFormatter =
                    IndexAxisValueFormatter(currenciesList)
                dialogBinding.root.rootView.bar_chart.description.isEnabled = false
                dialogBinding.root.rootView.bar_chart.xAxis.setDrawGridLines(true)
                dialogBinding.root.rootView.bar_chart.setPinchZoom(false)
                dialogBinding.root.rootView.bar_chart.setDrawBarShadow(false)
                dialogBinding.root.rootView.bar_chart.setDrawGridBackground(false)
                dialogBinding.root.rootView.bar_chart.xAxis.setDrawGridLines(false)
                dialogBinding.root.rootView.bar_chart.axisLeft.setDrawGridLines(false)
                dialogBinding.root.rootView.bar_chart.axisRight.setDrawGridLines(false)
                dialogBinding.root.rootView.bar_chart.axisRight.isEnabled = false
                dialogBinding.root.rootView.bar_chart.axisLeft.isEnabled = true
                dialogBinding.root.rootView.bar_chart.legend.isEnabled = true
                dialogBinding.root.rootView.bar_chart.legend.form = Legend.LegendForm.SQUARE
                dialogBinding.root.rootView.bar_chart.axisRight.setDrawLabels(true)
                dialogBinding.root.rootView.bar_chart.axisLeft.setDrawLabels(true)
                dialogBinding.root.rootView.bar_chart.setTouchEnabled(false)
                dialogBinding.root.rootView.bar_chart.isDoubleTapToZoomEnabled = false
                dialogBinding.root.rootView.bar_chart.xAxis.isEnabled = true
                dialogBinding.root.rootView.bar_chart.xAxis.position = XAxis.XAxisPosition.BOTH_SIDED
                dialogBinding.root.rootView.bar_chart.invalidate()
                dialog?.show()
            } else {
             context?.toast(getString(R.string.no_data_available))
            }
        })
    }
}
