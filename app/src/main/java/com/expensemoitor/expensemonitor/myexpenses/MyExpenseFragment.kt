package com.expensemoitor.expensemonitor.myexpenses



import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.expensemoitor.expensemonitor.R
import com.expensemoitor.expensemonitor.databinding.MyExpenseFragmentBinding
import android.view.MenuInflater
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2
import android.util.Log
import com.expensemoitor.expensemonitor.utilites.*
import kotlinx.android.synthetic.main.my_expense_fragment.*


class MyExpenseFragment : Fragment() {

    private lateinit var binding: MyExpenseFragmentBinding
    lateinit var viewModel: MyExpenseFragmentViewModel


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {


        binding = DataBindingUtil.inflate(inflater,R.layout.my_expense_fragment,container,false)





        val application = requireNotNull(this.activity).application

        val viewModelFactory = MyExpenseFragmentViewModelFactory(application)

        viewModel = ViewModelProviders.of(this,viewModelFactory)
            .get(MyExpenseFragmentViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this


        val tabLayout = binding.tabs
        val viewPager = binding.viewPager


        viewPager.adapter = PagerAdapter(this)


        // Set the text for each tab
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getTabTitle(position)
        }.attach()



        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int){
                super.onPageSelected(position)
                when(position.toString()){
                    "0"->{
                       binding.expenseTextView.text = expenseFormat("234000")
                       binding.dateTextView.text = displayCurrentDate()
                    }
                    "1"->{
                        binding.expenseTextView.text = expenseFormat("4344000")
                        binding.dateTextView.text = PrefManager.getStartOfWeek(application)+" "+"/"+" "+PrefManager.getEndOfWeek(application)
                    }
                    "2"->{
                        binding.expenseTextView.text = expenseFormat("63445000")
                        binding.dateTextView.text = "27-09-2019 - 27-10-2019"
                    }
                }
            }
        })



        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)
        setHasOptionsMenu(true)



        viewModel.navigateToMyExpense.observe(this, Observer {
            shouldNavigate->if (shouldNavigate){
            val navController = binding.root.findNavController()
            navController.navigate(R.id.action_myExpenseFragment_to_createNewExpenseFragment)
            viewModel.onNavigatedToMyExpense()
        }
        })


        return binding.root


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
        inflater.inflate(R.menu.main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share ->
                // do stuff
                return true

        }

        return false
    }








}
