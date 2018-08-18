package com.hanks.passcodeview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hanks.passcodeview.PasscodeView.PasscodeViewType.TYPE_CHECK_PASSCODE;
import static com.hanks.passcodeview.PasscodeView.PasscodeViewType.TYPE_SET_PASSCODE;

/**
 * PasscodeView
 * Created by hanks on 2017/4/11.
 */

public class PasscodeView extends FrameLayout implements View.OnClickListener
{
    private boolean mIsSecondInput;
    private String mLocalPasscode = "";
    private String mCurrentHash = "";
    private PasscodeViewListener mPasscodeListener;
    private ViewGroup mLayoutPasscode;
    private TextView mTvInpuTip;
    private TextView number0, number1, number2, number3, number4, number5, number6, number7, number8, number9;
    private ImageView numberB, numberOK;
    private ImageView mIvLock, mIvOK;
    private View cursor;

    private String mFirstInputTip = "Enter a passcode of 4 digits";
    private String mSecondInputTip = "Re-enter new passcode";
    private String mWrongLengthTip = "Enter a passcode of 4 digits";
    private String mWrongInputTip = "Passcode do not match";
    private String mCorrectInputTip = "Passcode is correct";

    private int passcodeLength = 4;
    private int correctStatusColor = 0xFF61C560; //0xFFFF0000
    private int wrongStatusColor = 0xFFF24055;
    private int normalStatusColor = 0xFFFFFFFF;
    private int numberTextColor = 0xFF747474;
    private int mPasscodeType = TYPE_SET_PASSCODE;

    private volatile boolean mIsPasscodeCorrect = false;
    private volatile String mPasscodeHash = null;

    public PasscodeView(@NonNull Context context)
    {
        this(context, null);
    }

    public PasscodeView(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);

        inflate(getContext(), R.layout.layout_passcode_view, this);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PasscodeView);
        try
        {
            mPasscodeType = typedArray.getInt(R.styleable.PasscodeView_passcodeViewType, mPasscodeType);
            passcodeLength = typedArray.getInt(R.styleable.PasscodeView_passcodeLength, passcodeLength);
            normalStatusColor = typedArray.getColor(R.styleable.PasscodeView_normalStateColor, normalStatusColor);
            wrongStatusColor = typedArray.getColor(R.styleable.PasscodeView_wrongStateColor, wrongStatusColor);
            correctStatusColor = typedArray.getColor(R.styleable.PasscodeView_correctStateColor, correctStatusColor);
            numberTextColor = typedArray.getColor(R.styleable.PasscodeView_numberTextColor, numberTextColor);
            mFirstInputTip = typedArray.getString(R.styleable.PasscodeView_firstInputTip);
            mSecondInputTip = typedArray.getString(R.styleable.PasscodeView_secondInputTip);
            mWrongLengthTip = typedArray.getString(R.styleable.PasscodeView_wrongLengthTip);
            mWrongInputTip = typedArray.getString(R.styleable.PasscodeView_wrongInputTip);
            mCorrectInputTip = typedArray.getString(R.styleable.PasscodeView_correctInputTip);
        }
        finally
        {
            typedArray.recycle();
        }

        mFirstInputTip = mFirstInputTip == null ? "Enter a passcode of 4 digits" : mFirstInputTip;
        mSecondInputTip = mSecondInputTip == null ? "Re-enter new passcode" : mSecondInputTip;
        mWrongLengthTip = mWrongLengthTip == null ? mFirstInputTip : mWrongLengthTip;
        mWrongInputTip = mWrongInputTip == null ? "Passcode do not match" : mWrongInputTip;
        mCorrectInputTip = mCorrectInputTip == null ? "Passcode is correct" : mCorrectInputTip;

        init();
    }

    private void init()
    {
        mLayoutPasscode = findViewById(R.id.layout_psd);
        mTvInpuTip = findViewById(R.id.tv_input_tip);
        cursor = findViewById(R.id.cursor);
        mIvLock = findViewById(R.id.iv_lock);
        mIvOK = findViewById(R.id.iv_ok);

        mTvInpuTip.setText(mFirstInputTip);

        number0 = findViewById(R.id.number0);
        number1 = findViewById(R.id.number1);
        number2 = findViewById(R.id.number2);
        number3 = findViewById(R.id.number3);
        number4 = findViewById(R.id.number4);
        number5 = findViewById(R.id.number5);
        number6 = findViewById(R.id.number6);
        number7 = findViewById(R.id.number7);
        number8 = findViewById(R.id.number8);
        number9 = findViewById(R.id.number9);
        numberOK = findViewById(R.id.numberOK);
        numberB = findViewById(R.id.numberB);

        number0.setOnClickListener(this);
        number1.setOnClickListener(this);
        number2.setOnClickListener(this);
        number3.setOnClickListener(this);
        number4.setOnClickListener(this);
        number5.setOnClickListener(this);
        number6.setOnClickListener(this);
        number7.setOnClickListener(this);
        number8.setOnClickListener(this);
        number9.setOnClickListener(this);

        numberB.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                deleteChar();
            }
        });
        numberOK.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                go();
            }
        });

        tintImageView(mIvLock, numberTextColor);
        tintImageView(numberB, numberTextColor);
        tintImageView(numberOK, numberTextColor);
        tintImageView(mIvOK, correctStatusColor);

        number0.setTag(0);
        number1.setTag(1);
        number2.setTag(2);
        number3.setTag(3);
        number4.setTag(4);
        number5.setTag(5);
        number6.setTag(6);
        number7.setTag(7);
        number8.setTag(8);
        number9.setTag(9);
        number0.setTextColor(numberTextColor);
        number1.setTextColor(numberTextColor);
        number2.setTextColor(numberTextColor);
        number3.setTextColor(numberTextColor);
        number4.setTextColor(numberTextColor);
        number5.setTextColor(numberTextColor);
        number6.setTextColor(numberTextColor);
        number7.setTextColor(numberTextColor);
        number8.setTextColor(numberTextColor);
        number9.setTextColor(numberTextColor);

    }

    @Override
    public void onClick(View view)
    {
        int number = (int) view.getTag();
        addChar(number);
    }

    /**
     * set  localPasscode
     *
     * @param localPasscode the code will to check
     */
    public PasscodeView setLocalPasscode(String localPasscode)
    {
        for (int i = 0; i < localPasscode.length(); i++)
        {
            char c = localPasscode.charAt(i);
            if (c < '0' || c > '9')
            {
                throw new RuntimeException("must be number digit");
            }
        }
        this.mLocalPasscode = localPasscode;
        this.mPasscodeType = TYPE_CHECK_PASSCODE;
        return this;
    }

    /**
     * set  currentHash
     *
     * @param currentHash
     */
    public PasscodeView setCurrentHash(String currentHash)
    {
        mCurrentHash = currentHash;
        mPasscodeType = TYPE_CHECK_PASSCODE;
        return this;
    }

    public PasscodeViewListener getListener()
    {
        return mPasscodeListener;
    }

    public PasscodeView setListener(PasscodeViewListener listener)
    {
        this.mPasscodeListener = listener;
        return this;
    }

    public String getmFirstInputTip()
    {
        return mFirstInputTip;
    }

    public PasscodeView setmFirstInputTip(String mFirstInputTip)
    {
        this.mFirstInputTip = mFirstInputTip;
        return this;
    }

    public String getmSecondInputTip()
    {
        return mSecondInputTip;
    }

    public PasscodeView setmSecondInputTip(String mSecondInputTip)
    {
        this.mSecondInputTip = mSecondInputTip;
        return this;
    }

    public String getWrongLengthTip()
    {
        return mWrongLengthTip;
    }

    public PasscodeView setWrongLengthTip(String wrongLengthTip)
    {
        this.mWrongLengthTip = wrongLengthTip;
        return this;
    }

    public String getWrongInputTip()
    {
        return mWrongInputTip;
    }

    public PasscodeView setWrongInputTip(String wrongInputTip)
    {
        this.mWrongInputTip = wrongInputTip;
        return this;
    }

    public String getCorrectInputTip()
    {
        return mCorrectInputTip;
    }

    public PasscodeView setCorrectInputTip(String correctInputTip)
    {
        this.mCorrectInputTip = correctInputTip;
        return this;
    }

    public int getPasscodeLength()
    {
        return passcodeLength;
    }

    public PasscodeView setPasscodeLength(int passcodeLength)
    {
        this.passcodeLength = passcodeLength;
        return this;
    }

    public int getCorrectStatusColor()
    {
        return correctStatusColor;
    }

    public PasscodeView setCorrectStatusColor(int correctStatusColor)
    {
        this.correctStatusColor = correctStatusColor;
        return this;
    }

    public int getWrongStatusColor()
    {
        return wrongStatusColor;
    }

    public PasscodeView setWrongStatusColor(int wrongStatusColor)
    {
        this.wrongStatusColor = wrongStatusColor;
        return this;
    }

    public int getNormalStatusColor()
    {
        return normalStatusColor;
    }

    public PasscodeView setNormalStatusColor(int normalStatusColor)
    {
        this.normalStatusColor = normalStatusColor;
        return this;
    }

    public int getNumberTextColor()
    {
        return numberTextColor;
    }

    public PasscodeView setNumberTextColor(int numberTextColor)
    {
        this.numberTextColor = numberTextColor;
        return this;
    }

    public @PasscodeViewType
    int getPasscodeType()
    {
        return mPasscodeType;
    }

    public PasscodeView setPasscodeType(@PasscodeViewType int passcodeType)
    {
        this.mPasscodeType = passcodeType;
        return this;
    }

    protected boolean equals(String val)
    {
        return mLocalPasscode.equals(val);
    }

    private void go()
    {
        if (mPasscodeType == TYPE_CHECK_PASSCODE)
        {
            checkThePasscode();
        }

        else if (mPasscodeType == TYPE_SET_PASSCODE)
        {
            setThePasscode();
        }
    }

    private void checkThePasscode()
    {
        if (TextUtils.isEmpty(mCurrentHash))
        {
            throw new RuntimeException("must set currentHash when type is TYPE_CHECK_PASSCODE");
        }

        final String passcode = getPasscodeFromView();
        if (passcode.length() != passcodeLength)
        {
            mTvInpuTip.setText(mWrongLengthTip);
            runTipTextAnimation();
            return;
        }

        mIsPasscodeCorrect = false;
        checkHashAsync(passcode); // will set mIsPasscodeCorrect value
        cursor.setTranslationX(0); // run cursor animaton meanwhile
        cursor.setVisibility(VISIBLE);
        runCursorAnimation(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                super.onAnimationEnd(animation);
                cursor.setVisibility(INVISIBLE);

                if (mIsPasscodeCorrect)
                {
                    runOkAnimation(mCurrentHash);
                }
                else
                {
                    runWrongAnimation();
                }
            }
        });
    }

    private void setThePasscode()
    {
        final String passcode = getPasscodeFromView();
        if (passcode.length() != passcodeLength)
        {
            mTvInpuTip.setText(mWrongLengthTip);
            runTipTextAnimation();
            return;
        }

        if (!mIsSecondInput)
        {
            mTvInpuTip.setText(mSecondInputTip);
            mLocalPasscode = passcode;
            clearChar();
            mIsSecondInput = true;
            return;
        }

        if (equals(passcode)) // match
        {
            calcHashAsync(passcode);
            cursor.setTranslationX(0); // run cursor animaton meanwhile
            cursor.setVisibility(VISIBLE);
            runCursorAnimation(new AnimatorListenerAdapter()
            {
                @Override
                public void onAnimationEnd(Animator animation)
                {
                    super.onAnimationEnd(animation);
                    cursor.setVisibility(INVISIBLE);

                    if (mPasscodeHash != null)
                    {
                        runOkAnimation(mPasscodeHash);
                    }
                    else
                    {
                        runWrongAnimation();
                    }
                }
            });
        }
        else
        {
            runWrongAnimation();
        }
    }

    private void calcHashAsync(final String passcode)
    {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new Runnable()
        {
            @Override
            public void run()
            {
                mPasscodeHash = hashPasscode(passcode);
            }
        });
        executorService.shutdown();
        try
        {
            // wait at for the hash control. 10 seconds tops.
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private void checkHashAsync(final String passcode)
    {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    mIsPasscodeCorrect = PasswordStorage.verifyPassword(passcode, mCurrentHash);
                }
                catch (PasswordStorage.CannotPerformOperationException | PasswordStorage.InvalidHashException e)
                {
                    e.printStackTrace();
                }
            }
        });
        executorService.shutdown();
        try
        {
            // wait at for the hash control. 10 seconds tops.
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public void runCursorAnimation(AnimatorListenerAdapter callback)
    {
        cursor.setTranslationX(0);
        cursor.setVisibility(VISIBLE);
        cursor.animate().translationX(mLayoutPasscode.getWidth()).setDuration(600).setListener(callback).start();
    }

    public void runWrongAnimation()
    {
        mTvInpuTip.setText(mWrongInputTip);
        setPSDViewBackgroundResource(wrongStatusColor);
        Animator animator = shakeAnimator(mLayoutPasscode);
        animator.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                super.onAnimationEnd(animation);
                setPSDViewBackgroundResource(normalStatusColor);
                if (mIsSecondInput && mPasscodeListener != null)
                {
                    mPasscodeListener.onFail();
                }
            }
        });
        animator.start();
    }

    public void runOkAnimation(final String hash)
    {
        setPSDViewBackgroundResource(correctStatusColor);
        mTvInpuTip.setText(mCorrectInputTip);
        mIvLock.animate().alpha(0).scaleX(0).scaleY(0).setDuration(500).start();
        mIvOK.animate().alpha(1).scaleX(1).scaleY(1).setDuration(500).setListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                super.onAnimationEnd(animation);
                if (mPasscodeListener != null)
                {
                    mLocalPasscode = null;
                    mPasscodeListener.onSuccess(hash);
                }
            }
        }).start();

    }

    private String hashPasscode(final String passcode)
    {
        try
        {
            String hash = PasswordStorage.createHash(passcode);
            return hash;
        }
        catch (PasswordStorage.CannotPerformOperationException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private void addChar(int number)
    {
        if (mLayoutPasscode.getChildCount() >= passcodeLength)
        {
            return;
        }
        CircleView psdView = new CircleView(getContext());
        int size = dpToPx(8);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(size, 0, size, 0);
        psdView.setLayoutParams(params);
        psdView.setColor(normalStatusColor);
        psdView.setTag(number);
        mLayoutPasscode.addView(psdView);
    }

    private int dpToPx(float valueInDp)
    {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }

    private void tintImageView(ImageView imageView, int color)
    {
        imageView.getDrawable().mutate().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    private void clearChar()
    {
        mLayoutPasscode.removeAllViews();
    }

    private void deleteChar()
    {
        int childCount = mLayoutPasscode.getChildCount();
        if (childCount <= 0)
        {
            return;
        }
        mLayoutPasscode.removeViewAt(childCount - 1);
    }

    public void runTipTextAnimation()
    {
        shakeAnimator(mTvInpuTip).start();
    }


    private Animator shakeAnimator(View view)
    {
        return ObjectAnimator.ofFloat(view, "translationX", 0, 25, -25, 25, -25, 15, -15, 6, -6, 0).setDuration(500);
    }

    private void setPSDViewBackgroundResource(int color)
    {
        int childCount = mLayoutPasscode.getChildCount();
        for (int i = 0; i < childCount; i++)
        {
            ((CircleView) mLayoutPasscode.getChildAt(i)).setColor(color);
        }
    }


    private String getPasscodeFromView()
    {
        StringBuilder sb = new StringBuilder();
        int childCount = mLayoutPasscode.getChildCount();
        for (int i = 0; i < childCount; i++)
        {
            View child = mLayoutPasscode.getChildAt(i);
            int num = (int) child.getTag();
            sb.append(num);
        }
        return sb.toString();
    }

    /**
     * The type for this passcodeView
     */
    @IntDef({TYPE_SET_PASSCODE, TYPE_CHECK_PASSCODE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PasscodeViewType
    {

        /**
         * set passcode, with twice input
         */
        int TYPE_SET_PASSCODE = 0;

        /**
         * check passcode, must pass the result as parameter {@link PasscodeView#setLocalPasscode(java.lang.String)}
         */
        int TYPE_CHECK_PASSCODE = 1;
    }

    public interface PasscodeViewListener
    {
        void onFail();

        void onSuccess(String number);
    }
}
