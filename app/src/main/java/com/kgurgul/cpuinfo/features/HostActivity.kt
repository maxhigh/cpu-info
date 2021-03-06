/*
 * Copyright 2017 KG Soft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kgurgul.cpuinfo.features

import android.annotation.SuppressLint
import android.databinding.DataBindingUtil
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.kgurgul.cpuinfo.R
import com.kgurgul.cpuinfo.databinding.ActivityHostLayoutBinding
import com.kgurgul.cpuinfo.utils.runOnApiAbove
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import timber.log.Timber
import javax.inject.Inject

/**
 * Base activity which is a host for whole application.
 *
 * @author kgurgul
 */
class HostActivity : AppCompatActivity(), HasSupportFragmentInjector {

    private val CURRENT_PAGE_ID_KEY = "CURRENT_PAGE_ID_KEY"

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var navigationController: NavigationController

    private lateinit var binding: ActivityHostLayoutBinding

    private var currentItemId = R.id.hardware

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_host_layout)
        setSupportActionBar(binding.toolbar)
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            handleMenuNavigation(menuItem)
        }
        runOnApiAbove(Build.VERSION_CODES.M, {
            // Processes cannot be listed above M
            val menu = binding.navigationView.menu
            menu.findItem(R.id.processes).isVisible = false
        }, {})
        val actionBarDrawerToggle = getDrawerToggle()
        binding.drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        if (savedInstanceState == null) {
            navigationController.navigateToInfo()
        } else {
            currentItemId = savedInstanceState.getInt(CURRENT_PAGE_ID_KEY)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CURRENT_PAGE_ID_KEY, currentItemId)
        super.onSaveInstanceState(outState)
    }

    /**
     * Handle all navigation callbacks from NavigationDrawer. It also block possibility to open the
     * same fragment twice
     */
    private fun handleMenuNavigation(menuItem: MenuItem): Boolean {
        binding.drawerLayout.closeDrawers()

        if (currentItemId == menuItem.itemId) {
            return true
        }
        currentItemId = menuItem.itemId

        when (menuItem.itemId) {
            R.id.hardware -> {
                Handler().postDelayed({
                    navigationController.navigateToInfo()
                    setToolbarTitleAndElevation(getString(R.string.hardware))
                }, 500)
                return true
            }
            R.id.applications -> {
                Handler().postDelayed({
                    navigationController.navigateToApplications()
                    setToolbarTitleAndElevation(getString(R.string.applications))
                }, 500)
                return true
            }
            R.id.processes -> {
                Handler().postDelayed({
                    navigationController.navigateToProcesses()
                    setToolbarTitleAndElevation(getString(R.string.processes))
                }, 500)
                return true
            }
            R.id.temp -> {
                Handler().postDelayed({
                    navigationController.navigateToTemperature()
                    setToolbarTitleAndElevation(getString(R.string.temperature))
                }, 500)
                return true
            }
            R.id.settings -> {
                Handler().postDelayed({
                    navigationController.navigateToSettings()
                    setToolbarTitleAndElevation(getString(R.string.settings))
                }, 500)
                return true
            }
            else -> {
                Timber.e("Bad nav option")
                return true
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val homeFragment = supportFragmentManager
                .findFragmentByTag(NavigationController.HOME_FRAGMENT_TAG)
        if (homeFragment != null && homeFragment.isVisible) {
            currentItemId = R.id.hardware
            setToolbarTitleAndElevation(getString(R.string.hardware))
        }
    }

    /**
     * Set toolbar title and manage elevation in case of L+ devices and TabLayout
     */
    @SuppressLint("NewApi")
    private fun setToolbarTitleAndElevation(title: String) {
        binding.toolbar.title = title
        runOnApiAbove(Build.VERSION_CODES.KITKAT_WATCH, {
            if (currentItemId == R.id.hardware) {
                binding.toolbar.elevation = 0f
            } else {
                binding.toolbar.elevation = resources.getDimension(R.dimen.elevation_height)
            }
        }, {})
    }

    /**
     * Simple drawer toggle without extra logic
     */
    private fun getDrawerToggle(): ActionBarDrawerToggle {
        return ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar,
                R.string.open_drawer, R.string.close_drawer)
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment>
            = dispatchingAndroidInjector
}