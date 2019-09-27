package com.expensemoitor.expensemonitor.registeruser


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController

import com.expensemoitor.expensemonitor.R
import com.expensemoitor.expensemonitor.databinding.RegisterationUserFragmentBinding


class RegisterationUserFragment : Fragment() {

    private lateinit var binding: RegisterationUserFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment

        binding = DataBindingUtil.inflate(inflater,R.layout.registeration_user_fragment,container,false)

        val application = requireNotNull(this.activity).application

        val viewModelFactory = RegisterationUserViewModelFactory(application)

        val viewModel = ViewModelProviders.of(this,viewModelFactory)
            .get(RegisterationUserViewModel::class.java)



        binding.viewModel = viewModel

        binding.lifecycleOwner = this



        viewModel.genderSelected.observe(this, Observer { isSelected ->
            if (!isSelected) {
                Toast.makeText(context,"Please Select Option",Toast.LENGTH_LONG).show()
                viewModel.genderAlreadySelected()
              }
            }
        )

        viewModel.displayMsg.observe(
            this,
            Observer { displayMsg ->
                if (displayMsg) {
                    Toast.makeText(context,"Please Check Internet Connections",Toast.LENGTH_LONG).show()
                    viewModel.internetIsAvailable()
                }
            }
        )

        viewModel.navigateToMyExpenseFragment.observe(this, Observer{shouldNavigate->
             if(shouldNavigate){
                 val navController = binding.root.findNavController()
                 navController.navigate(R.id.action_registerationUserFragment_to_myExpenseFragment)
             }
        })


        return  binding.root
    }


}
