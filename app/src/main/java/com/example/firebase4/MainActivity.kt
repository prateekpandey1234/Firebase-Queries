package com.example.firebase4

import android.app.Person
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.StringBuilder

class MainActivity : AppCompatActivity() {
    //the database in firebase is stored as a firestore collection whose reference is important to access our firebase database
    private val personcollectionref = Firebase.firestore.collection("persons")//need to add ktx dependency for firebase-firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnUploadData.setOnClickListener {
            val firstName = etFirstName.text.toString()
            val lastName = etLastName.text.toString()
            val age = etAge.text.toString().toInt()
            val person = person(firstName, lastName, age)
            savePerson(person)
        }
//        subscribeToRealtimeUpdates()
        btnRetrieveData.setOnClickListener {
            retrievePersons()
        }
        //here we create the function to update our database
        btnUpdatePerson.setOnClickListener {
            val oldPerson = getOldPerson()
            val newPersonMap = getNewPersonMap()
            Log.d("nigga","$newPersonMap \n $oldPerson")
            updatePerson(oldPerson, newPersonMap)
        }
        btnDeletePerson.setOnClickListener {
            val oldPerson = getOldPerson()
            deletePerson(oldPerson)
        }
//        button.setOnClickListener{
//            changename("prateek","pandey", "sDgFeJbHbnsSq1rrs5x3")
//        }
    }
    //here we save our data class to our firestore using collection path reference we made up there
    private fun savePerson(person: person) = CoroutineScope(Dispatchers.IO).launch {
        try {
            personcollectionref.add(person).await()
            //but we need to create database also in our firebase-firestore project......
            //we nedd to select our region from where our most of the users will be
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Successfully saved data.", Toast.LENGTH_LONG).show()
            }
        } catch(e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun retrievePersons()= CoroutineScope(Dispatchers.IO).launch {
        val fromAge = etFrom.text.toString().toInt()
        val toAge = etTo.text.toString().toInt()
        try{
            val QuerySnapshot = personcollectionref//we can also add query to the realtime database
                .whereGreaterThan("age", fromAge)//here we add the field in which we want to query
                .whereLessThan("age", toAge)
                .orderBy("age")//here we tell the order in which data should be ascending/descending
                .get()
                .await()//here we retrieve our data from firestore in form of a QuerySnapshot
            val documents  = QuerySnapshot.documents
            val sb = StringBuilder()//A mutable sequence of characters. String builder can be used to efficiently
                                    //perform multiple string manipulation operations.
            for (docs in documents){
                val person = docs.toObject<person>()//we here return generic type of object
                sb.append("$person\n")
            }
            withContext(Dispatchers.Main) {
                tvPersons.text = sb.toString()
            }
        }catch (e:Exception){
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }
    //the SnapShotListner inside the Firestore allows us to handle Realtime database which means we can access any type of change made
    //in our database for our app
    private fun subscribeToRealtimeUpdates() {
        personcollectionref.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            //below we check here whether there is an error or not
            firebaseFirestoreException?.let {
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }
            querySnapshot?.let {
                val sb = StringBuilder()
                for(document in it) {
                    val person = document.toObject<person>()
                    sb.append("$person\n")
                }
                tvPersons.text = sb.toString()
            }
        }
    }
    private fun getOldPerson(): person {
        val firstName = etFirstName.text.toString()
        val lastName = etLastName.text.toString()
        val age = etAge.text.toString().toInt()
        return person(firstName, lastName, age)
    }
    //we should prefer using a mutablemap to access or update our firestore database
    private fun getNewPersonMap(): Map<String, Any> {
        val firstName = etNewFirstName.text.toString()
        val lastName = etNewLastName.text.toString()
        val age = etNewAge.text.toString()
        val map = mutableMapOf<String, Any>()
        if(firstName.isNotEmpty()) {
            map["firstName"] = firstName
        }
        if(lastName.isNotEmpty()) {
            map["lastName"] = lastName
        }
        if(age.isNotEmpty()) {
            map["age"] = age.toInt()
        }
        Log.d("person","$map")
        return map
    }
    private fun updatePerson(person1: person, newPersonMap: Map<String, Any>) = CoroutineScope(Dispatchers.IO).launch {
        Log.d("update","$person1")
        val personQuery = personcollectionref
            .whereEqualTo("firstname",person1.firstname)
            .whereEqualTo("lastname" , person1.lastname)
            .get()
            .await()
        Log.d("lol","${personQuery.documents}")
        if(personQuery.documents.isNotEmpty()) {
            for(document in personQuery) {
                try {
                    Log.d("pop","${personQuery.documents}")
                    //personCollectionRef.document(document.id).update("age", newAge).await()
                    personcollectionref.document(document.id).set(//ACCESSING ID FOR THE DOCUMENT
                        newPersonMap,//if we only passed our map data...other fields which don't have any value will be deleted
                        //hence we pass which way we want to set our data....this one merges the old data with new
                    ).await()
                    Log.d("pop","${personQuery.documents}")
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "No persons matched the query.", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun deletePerson(person: person) = CoroutineScope(Dispatchers.IO).launch {
        val personQuery = personcollectionref
            .whereEqualTo("firstName", person.firstname)
            .whereEqualTo("lastName", person.lastname)
            .whereEqualTo("age", person.age)
            .get()
            .await()
        if(personQuery.documents.isNotEmpty()) {
            for(document in personQuery) {
                try {
                    personcollectionref.document(document.id).delete().await()//deleting the whole person/object
                    /*personCollectionRef.document(document.id).update(mapOf(    //deleting a single field
                        "firstName" to FieldValue.delete()
                    )).await()*/
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "No persons matched the query.", Toast.LENGTH_LONG).show()
            }
        }
    }
    //the following method of batched write is much preferred to update or delete anything from firestore
    //we just need to give id of that document and everything else is same
    // a batched write is much more efficient way to write/delete database because it allows to make any change
    //in database all at one time and if any of the batch write fails...all other writes will be rolled back
    //it helps to avoid mixed up database values
    //bratched writes fails when clients tries to change documents when the writing starts
    private fun changename(
        firstname:String,
        lastname:String,
        perosnId:String
    )= CoroutineScope(Dispatchers.IO).launch {
        try{
            Firebase.firestore.runBatch {batch ->
                val perosnRef = personcollectionref.document(perosnId)
                batch.update(perosnRef,"firstname",firstname)
                batch.update(perosnRef,"lastname",lastname)
            }.await()

        }catch(e:Exception){
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }
    //transactions work very different way..first the database sends the documents to client to read
    //and then the client does some logical operation and asks for some changes and in that if there is no change
    //in the document the database will commit changes otherwise it will repeat the whole process
    private fun bday(
        perosnId:String
    )= CoroutineScope(Dispatchers.IO).launch {
        try {
            Firebase.firestore.runTransaction { transction->
                val personrf = personcollectionref.document(perosnId)
                val person = transction.get(personrf)
                val newage = person["age"] as Long +1
                transction.update(personrf,"age",newage)
                null
            }.await()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }

        }
    }
}
