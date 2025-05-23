package dev.rx.app2proxy

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val rulesUpdateListener: RulesUpdateListener
) : FragmentStateAdapter(fragmentActivity) {
    
    private val fragments = mutableMapOf<Int, Fragment>()
    
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        val fragment = when (position) {
            0 -> AppListFragment()
            1 -> RulesManagerFragment().apply {
                setRulesUpdateListener(rulesUpdateListener)
            }
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
        fragments[position] = fragment
        return fragment
    }

    fun getFragment(position: Int): Fragment? {
        return fragments[position]
    }
}
