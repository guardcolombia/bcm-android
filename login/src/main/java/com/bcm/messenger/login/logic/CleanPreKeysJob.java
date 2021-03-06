package com.bcm.messenger.login.logic;

import android.content.Context;
import android.util.Log;

import com.bcm.messenger.common.AccountContext;
import com.bcm.messenger.common.crypto.MasterSecret;
import com.bcm.messenger.common.crypto.PreKeyUtil;
import com.bcm.messenger.common.crypto.storage.SignalProtocolStoreImpl;
import com.bcm.messenger.common.jobs.MasterSecretJob;
import com.bcm.messenger.common.jobs.requirements.MasterSecretRequirement;

import org.whispersystems.jobqueue.JobParameters;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyStore;
import org.whispersystems.signalservice.api.push.exceptions.NonSuccessfulResponseCodeException;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class CleanPreKeysJob extends MasterSecretJob {

    public static interface SignedPreKeyStoreFactory {
        public SignedPreKeyStore create();
    }

    private static final String TAG = CleanPreKeysJob.class.getSimpleName();

    private static final long ARCHIVE_AGE = TimeUnit.DAYS.toMillis(7);

    private transient SignedPreKeyStoreFactory signedPreKeyStoreFactory;

    public CleanPreKeysJob(Context context, AccountContext accountContext) {
        super(context, accountContext, JobParameters.newBuilder()
                .withGroupId(CleanPreKeysJob.class.getSimpleName())
                .withRequirement(new MasterSecretRequirement(context, accountContext))
                .withRetryCount(5)
                .create());
        this.signedPreKeyStoreFactory = new SignedPreKeyStoreFactory() {
            @Override
            public SignedPreKeyStore create() {
                return new SignalProtocolStoreImpl(context, accountContext);
            }
        };
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun(MasterSecret masterSecret) throws IOException {
        try {
            Log.w(TAG, "Cleaning prekeys...");

            int activeSignedPreKeyId = PreKeyUtil.getActiveSignedPreKeyId(context, accountContext);
            SignedPreKeyStore signedPreKeyStore = signedPreKeyStoreFactory.create();

            if (activeSignedPreKeyId < 0) return;

            SignedPreKeyRecord currentRecord = signedPreKeyStore.loadSignedPreKey(activeSignedPreKeyId);
            List<SignedPreKeyRecord> allRecords = signedPreKeyStore.loadSignedPreKeys();
            LinkedList<SignedPreKeyRecord> oldRecords = removeRecordFrom(currentRecord, allRecords);

            Collections.sort(oldRecords, new SignedPreKeySorter());

            Log.w(TAG, "Active signed prekey: " + activeSignedPreKeyId);
            Log.w(TAG, "Old signed prekey record count: " + oldRecords.size());

            boolean foundAgedRecord = false;

            for (SignedPreKeyRecord oldRecord : oldRecords) {
                long archiveDuration = System.currentTimeMillis() - oldRecord.getTimestamp();

                if (archiveDuration >= ARCHIVE_AGE) {
                    if (!foundAgedRecord) {
                        foundAgedRecord = true;
                    } else {
                        Log.w(TAG, "Removing signed prekey record: " + oldRecord.getId() + " with timestamp: " + oldRecord.getTimestamp());
                        signedPreKeyStore.removeSignedPreKey(oldRecord.getId());
                    }
                }
            }
        } catch (InvalidKeyIdException e) {
            Log.w(TAG, e);
        }
    }

    @Override
    public boolean onShouldRetryThrowable(Exception throwable) {
        if (throwable instanceof NonSuccessfulResponseCodeException) return false;
        if (throwable instanceof PushNetworkException) return true;
        return false;
    }

    @Override
    public void onCanceled() {
        Log.w(TAG, "Failed to execute clean signed prekeys task.");
    }

    private LinkedList<SignedPreKeyRecord> removeRecordFrom(SignedPreKeyRecord currentRecord,
                                                            List<SignedPreKeyRecord> records) {
        LinkedList<SignedPreKeyRecord> others = new LinkedList<>();

        for (SignedPreKeyRecord record : records) {
            if (record.getId() != currentRecord.getId()) {
                others.add(record);
            }
        }

        return others;
    }

    private static class SignedPreKeySorter implements Comparator<SignedPreKeyRecord> {
        @Override
        public int compare(SignedPreKeyRecord lhs, SignedPreKeyRecord rhs) {
            if (lhs.getTimestamp() > rhs.getTimestamp()) return -1;
            else if (lhs.getTimestamp() < rhs.getTimestamp()) return 1;
            else return 0;
        }
    }

}
