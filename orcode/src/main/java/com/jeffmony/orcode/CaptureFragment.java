package com.jeffmony.orcode;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jeffmony.orcode.camera.CameraManager;

public class CaptureFragment extends Fragment implements OnCaptureCallback {

    public static final String KEY_RESULT = Intents.Scan.RESULT;

    private View mRootView;
    private SurfaceView mSurfaceView;
    private ViewfinderView mViewfinderView;
    private View mIvTorch;

    private CaptureHelper mCaptureHelper;

    public static CaptureFragment newInstance() {

        Bundle args = new Bundle();

        CaptureFragment fragment = new CaptureFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int layoutId = getLayoutId();
        if(isContentView(layoutId)){
            mRootView = inflater.inflate(getLayoutId(),container,false);
        }
        initUI();
        return mRootView;
    }

    /**
     * 初始化
     */
    public void initUI(){
        mSurfaceView = mRootView.findViewById(getSurfaceViewId());
        mViewfinderView = mRootView.findViewById(getViewfinderViewId());
        int ivTorchId = getIvTorchId();
        if(ivTorchId != 0){
            mIvTorch = mRootView.findViewById(ivTorchId);
            mIvTorch.setVisibility(View.INVISIBLE);
        }
        mCaptureHelper = new CaptureHelper(this,mSurfaceView, mViewfinderView, mIvTorch);
        mCaptureHelper.setOnCaptureCallback(this);
    }

    /**
     * 返回true时会自动初始化{@link #mRootView}，返回为false时需自己去通过{@link #setRootView(View)}初始化{@link #mRootView}
     * @param layoutId
     * @return 默认返回true
     */
    public boolean isContentView(@LayoutRes int layoutId){
        return true;
    }

    /**
     * 布局id
     * @return
     */
    public int getLayoutId(){
        return R.layout.zxl_capture;
    }

    /**
     * {@link ViewfinderView} 的 id
     * @return
     */
    public int getViewfinderViewId(){
        return R.id.viewfinderView;
    }

    /**
     * 预览界面{@link #mSurfaceView} 的id
     * @return
     */
    public int getSurfaceViewId(){
        return R.id.surfaceView;
    }

    /**
     * 获取 {@link #mIvTorch} 的ID
     * @return  默认返回{@code R.id.ivTorch}, 如果不需要手电筒按钮可以返回0
     */
    public int getIvTorchId(){
        return R.id.ivTorch;
    }

    /**
     * Get {@link CaptureHelper}
     * @return {@link #mCaptureHelper}
     */
    public CaptureHelper getCaptureHelper(){
        return mCaptureHelper;
    }

    /**
     * Get {@link com.jeffmony.orcode.camera.CameraManager} use {@link #getCaptureHelper()#getCameraManager()}
     * @return {@link #mCaptureHelper#getCameraManager()}
     */
    @Deprecated
    public CameraManager getCameraManager(){
        return mCaptureHelper.getCameraManager();
    }

    //--------------------------------------------

    public View getRootView() {
        return mRootView;
    }

    public void setRootView(View rootView) {
        this.mRootView = rootView;
    }


    //--------------------------------------------

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mCaptureHelper.onCreate();
    }

    @Override
    public void onResume() {
        super.onResume();
        mCaptureHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mCaptureHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCaptureHelper.onDestroy();
    }

    /**
     * 接收扫码结果回调
     * @param result 扫码结果
     * @return 返回true表示拦截，将不自动执行后续逻辑，为false表示不拦截，默认不拦截
     */
    @Override
    public boolean onResultCallback(String result) {
        return false;
    }

}