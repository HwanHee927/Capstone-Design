package com.wonchihyeon.giveback.GetBoard

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.wonchihyeon.giveback.Chatting.ChatActivity
import com.wonchihyeon.giveback.R
import com.wonchihyeon.giveback.databinding.ActivityGetBoardInsideBinding
import com.wonchihyeon.giveback.utils.FBAuth
import com.wonchihyeon.giveback.utils.FBRef
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

// 게시글 보기 페이지
class GetBoardInsideActivity : AppCompatActivity() {

    private val TAG = GetBoardInsideActivity::class.java.simpleName

    private lateinit var binding: ActivityGetBoardInsideBinding

    private lateinit var key:String

    private lateinit var writerEmail: String
    private lateinit var writerUid: String

    // 이미지 최대 개수
    var count = 3

    //View Pager객체 선언
    internal lateinit var viewPager: ViewPager2

    private val boardKeyList = mutableListOf<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_get_board_inside)

        // boardSettingIcon을 클릭하면 custom_dialog layout이 나오도록 이벤트 처리
        binding.boardSettingIcon.setOnClickListener {
            showDialog()
        }


        key = intent.getStringExtra("key").toString()
        getBoardData(key)

        //View Pager레이아웃을 찾아서 연결
        viewPager= findViewById(R.id.view_pager) as ViewPager2

        for(i in 0 until 3) {
            boardKeyList.add("${key}${i}")
        }

        //adapter으로 연결
        val adapter= GetViewPagerAdapter(this, boardKeyList, key)
        viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL // 방향을 가로로

        viewPager.adapter = adapter

        viewPager.offscreenPageLimit = 5 // 몇 개의 페이지를 미리 로드 해둘것인지

        binding.springDotsIndicator.setViewPager2(viewPager) // indicator 설정

        binding.manageNumber.text = "관리번호 : ${key}"

        writerEmail = intent.getStringExtra("email").toString()
        writerUid = intent.getStringExtra("uid").toString()

        // 채팅하기 버튼 클릭 시
        binding.chatBtn.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("uid", writerUid)
            intent.putExtra("email", writerEmail)

            startActivity(intent)
        }

        getImageData(key, count)
    }

    // 만든 custom_dialog를 띄우는 showDialog() 함수 생성
    private fun showDialog() {
        val mDialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog, null)
        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)
            .setTitle("게시글 수정/삭제")

        val alertDialog = mBuilder.show()
        // 수정버튼을 클릭했을 때
        alertDialog.findViewById<Button>(R.id.editBtn)?.setOnClickListener {
            Toast.makeText(this, "수정 버튼을 눌렀습니다.", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, GetBoardEditActivity::class.java)
            intent.putExtra("key", key)
            startActivity(intent)

            alertDialog.dismiss()
        }
        // 삭제버튼을 클릭했을 때
        alertDialog.findViewById<Button>(R.id.removeBtn)?.setOnClickListener {

            val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setIcon(R.drawable.message)
                .setTitle("해당 게시글을 삭제하겠습니까?")
                .setPositiveButton("확인") { dialog, which ->
                    FBRef.getboardRef.child(key).removeValue()
                    Toast.makeText(this, "삭제완료", Toast.LENGTH_SHORT).show()

                    finish()
                }
                .setNegativeButton("취소", null)
                .create()
            alertDialog.show()
        }
    }
    // 이미지 데이터를 받아오는 함수
    private fun getImageData(key: String, count: Int) {

        // 사진을 차례대로 받아서 이미지뷰0~5번에 뿌림
        for (i in 0 until count) {

            val storageReference = Firebase.storage.reference.child("${key}${i}.png")

            val imageViewFromFB = findViewById<ImageView>(resources.getIdentifier("getImageArea$i", "id", packageName))

            storageReference.downloadUrl.addOnCompleteListener(OnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUrl = task.result
                    if (imageViewFromFB != null) {
                        Glide.with(this)
                            .load(downloadUrl)
                            .into(imageViewFromFB)
                    }
                } else {
                    imageViewFromFB?.visibility = View.GONE
                }
            })

        }
    }

    // 게시글을 가져오는 함수
    private fun getBoardData(key: String) {
        val postListner = object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val dataModel = dataSnapshot.getValue(GetBoardModel::class.java)

                if (dataModel != null) {

                    binding.emailArea.setText("습득자: ${dataModel.email}")
                    binding.titleArea.setText("습득물명: ${dataModel.title}")
                    binding.categoryArea.setText("카테고리명: ${dataModel.category}")
                    binding.getDateArea.setText("습득날짜: ${dataModel.getDate}")
                    binding.getlocationArea.setText("습득위치: ${dataModel.getLocation} ${dataModel.getdetailLocation}")
                    binding.keeplocationArea.setText("보관위치: ${dataModel.keepLocation} ${dataModel.keepdetailLocation}")
                    binding.contentArea.setText("상세내용 : ${dataModel.content}")
                }

                val myUid = FBAuth.getUid()
                val writerUid = dataModel?.uid
                if(myUid.equals(writerUid)){
                    binding.boardSettingIcon.isVisible = true
                } else {

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
            }
        }
        FBRef.getboardRef.child(key).addValueEventListener(postListner)
    }
}