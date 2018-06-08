package com.ivianuu.epoxyextensions.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.airbnb.epoxy.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        object : EpoxyController() {
            override fun buildModels() {
                finalEpoxy {
                }
            }
        }
    }
}

@EpoxyModelClass(layout = R.layout.abc_action_menu_layout)
abstract class FinalEpoxyModel : MyEpoxyModelBaseClass5() {
    @EpoxyAttribute var finalAttribute = ""
}

@EpoxyModelClass(layout = R.layout.abc_action_menu_layout)
abstract class MyEpoxyModelBaseClass5 : MyEpoxyModelBaseClass4() {
    @EpoxyAttribute var baseClass5Attribute: String = ""
}

abstract class MyEpoxyModelBaseClass4 : MyEpoxyModelBaseClass3() {
    @EpoxyAttribute var baseClass4Attribute: String = ""
}

abstract class MyEpoxyModelBaseClass3 : MyEpoxyModelBaseClass2() {
    @EpoxyAttribute var baseClass3Attribute: String = ""
}

@EpoxyModelClass(layout = R.layout.abc_action_menu_item_layout)
abstract class MyEpoxyModelBaseClass2 : MyEpoxyModelBaseClass1() {
    @EpoxyAttribute var baseClass2Attribute: Boolean? = null
}

abstract class MyEpoxyModelBaseClass1 : EpoxyModelWithHolder<MyEpoxyModelBaseClass1.Holder>() {
    @EpoxyAttribute lateinit var baseClass1Attribute: String
    class Holder : EpoxyHolder() {
        override fun bindView(itemView: View?) {
        }
    }
}