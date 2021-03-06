/**
 * Copyright (C) 2011 Whisper Systems
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.bcm.messenger.common.deprecated;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bcm.messenger.common.AccountContext;
import com.bcm.messenger.common.crypto.MasterSecret;

import org.whispersystems.libsignal.util.guava.Optional;

import java.util.HashSet;
import java.util.Set;

import com.bcm.messenger.utility.logger.ALog;

@Deprecated
public class MmsSmsDatabase extends Database {

    private static final String TAG = MmsSmsDatabase.class.getSimpleName();

    public static final String TRANSPORT = "transport_type";
    public static final String MMS_TRANSPORT = "mms";
    public static final String SMS_TRANSPORT = "sms";

    private static final String[] PROJECTION = {MmsSmsColumns.ID, MmsSmsColumns.UNIQUE_ROW_ID,
            SmsDatabase.BODY, SmsDatabase.TYPE,
            MmsSmsColumns.THREAD_ID,
            SmsDatabase.ADDRESS, SmsDatabase.ADDRESS_DEVICE_ID, SmsDatabase.SUBJECT,
            MmsSmsColumns.NORMALIZED_DATE_SENT,
            MmsSmsColumns.NORMALIZED_DATE_RECEIVED,
            MmsDatabase.MESSAGE_TYPE, MmsDatabase.MESSAGE_BOX,
            SmsDatabase.STATUS, MmsDatabase.PART_COUNT,
            MmsDatabase.CONTENT_LOCATION, MmsDatabase.TRANSACTION_ID,
            MmsDatabase.MESSAGE_SIZE, MmsDatabase.EXPIRY,
            MmsDatabase.STATUS,
            MmsSmsColumns.DELIVERY_RECEIPT_COUNT,
            MmsSmsColumns.READ_RECEIPT_COUNT,
            MmsSmsColumns.MISMATCHED_IDENTITIES,
            MmsDatabase.NETWORK_FAILURE,
            MmsSmsColumns.SUBSCRIPTION_ID,
            MmsSmsColumns.EXPIRES_IN,
            MmsSmsColumns.EXPIRE_STARTED,
            MmsSmsColumns.NOTIFIED,
            MmsSmsColumns.READ,
            SmsDatabase.DURATION,
            SmsDatabase.COMMUNICATION_TYPE,
            SmsDatabase.PAYLOAD_TYPE,
            TRANSPORT,
            AttachmentDatabase.ATTACHMENT_ID_ALIAS,
            AttachmentDatabase.UNIQUE_ID,
            AttachmentDatabase.MMS_ID,
            AttachmentDatabase.SIZE,
            AttachmentDatabase.FILE_NAME,
            AttachmentDatabase.DATA,
            AttachmentDatabase.THUMBNAIL,
            AttachmentDatabase.CONTENT_TYPE,
            AttachmentDatabase.CONTENT_LOCATION,
            AttachmentDatabase.DIGEST,
            AttachmentDatabase.FAST_PREFLIGHT_ID,
            AttachmentDatabase.VOICE_NOTE,
            AttachmentDatabase.CONTENT_DISPOSITION,
            AttachmentDatabase.NAME,
            AttachmentDatabase.TRANSFER_STATE,
            AttachmentDatabase.DURATION,
            AttachmentDatabase.URL,
            AttachmentDatabase.THUMBNAIL_ASPECT_RATIO};

    public static String[] getMmsProjection() {

        return new String[]{MmsDatabase.DATE_SENT + " AS " + MmsSmsColumns.NORMALIZED_DATE_SENT,
                MmsDatabase.DATE_RECEIVED + " AS " + MmsSmsColumns.NORMALIZED_DATE_RECEIVED,
                MmsDatabase.TABLE_NAME + "." + MmsDatabase.ID + " AS " + MmsSmsColumns.ID,
                "'MMS::' || " + MmsDatabase.TABLE_NAME + "." + MmsDatabase.ID
                        + " || '::' || " + MmsDatabase.DATE_SENT
                        + " AS " + MmsSmsColumns.UNIQUE_ROW_ID,
                AttachmentDatabase.TABLE_NAME + "." + AttachmentDatabase.ROW_ID + " AS " + AttachmentDatabase.ATTACHMENT_ID_ALIAS,
                SmsDatabase.BODY, MmsSmsColumns.READ, MmsSmsColumns.THREAD_ID,
                SmsDatabase.TYPE, SmsDatabase.ADDRESS, SmsDatabase.ADDRESS_DEVICE_ID, SmsDatabase.SUBJECT, MmsDatabase.MESSAGE_TYPE,
                MmsDatabase.MESSAGE_BOX, SmsDatabase.STATUS, MmsDatabase.PART_COUNT,
                MmsDatabase.CONTENT_LOCATION, MmsDatabase.TRANSACTION_ID,
                MmsDatabase.MESSAGE_SIZE, MmsDatabase.EXPIRY, MmsDatabase.STATUS,
                MmsSmsColumns.DELIVERY_RECEIPT_COUNT, MmsSmsColumns.READ_RECEIPT_COUNT,
                MmsSmsColumns.MISMATCHED_IDENTITIES,
                MmsSmsColumns.SUBSCRIPTION_ID, MmsSmsColumns.EXPIRES_IN, MmsSmsColumns.EXPIRE_STARTED,
                MmsSmsColumns.NOTIFIED, MmsSmsColumns.READ,
                MmsDatabase.NETWORK_FAILURE, TRANSPORT, SmsDatabase.DURATION, SmsDatabase.COMMUNICATION_TYPE,
                SmsDatabase.PAYLOAD_TYPE,
                AttachmentDatabase.UNIQUE_ID,
                AttachmentDatabase.MMS_ID,
                AttachmentDatabase.SIZE,
                AttachmentDatabase.FILE_NAME,
                AttachmentDatabase.DATA,
                AttachmentDatabase.THUMBNAIL,
                AttachmentDatabase.CONTENT_TYPE,
                AttachmentDatabase.CONTENT_LOCATION,
                AttachmentDatabase.DIGEST,
                AttachmentDatabase.FAST_PREFLIGHT_ID,
                AttachmentDatabase.VOICE_NOTE,
                AttachmentDatabase.CONTENT_DISPOSITION,
                AttachmentDatabase.NAME,
                AttachmentDatabase.TRANSFER_STATE,
                AttachmentDatabase.DURATION,
                AttachmentDatabase.URL,
                AttachmentDatabase.THUMBNAIL_ASPECT_RATIO};

    }

    public static String[] getSmsProjection() {

        return new String[]{SmsDatabase.DATE_SENT + " AS " + MmsSmsColumns.NORMALIZED_DATE_SENT,
                SmsDatabase.DATE_RECEIVED + " AS " + MmsSmsColumns.NORMALIZED_DATE_RECEIVED,
                MmsSmsColumns.ID,
                "'SMS::' || " + MmsSmsColumns.ID
                        + " || '::' || " + SmsDatabase.DATE_SENT
                        + " AS " + MmsSmsColumns.UNIQUE_ROW_ID,
                "NULL AS " + AttachmentDatabase.ATTACHMENT_ID_ALIAS,
                SmsDatabase.BODY, MmsSmsColumns.READ, MmsSmsColumns.THREAD_ID,
                SmsDatabase.TYPE, SmsDatabase.ADDRESS, SmsDatabase.ADDRESS_DEVICE_ID, SmsDatabase.SUBJECT, MmsDatabase.MESSAGE_TYPE,
                MmsDatabase.MESSAGE_BOX, SmsDatabase.STATUS, MmsDatabase.PART_COUNT,
                MmsDatabase.CONTENT_LOCATION, MmsDatabase.TRANSACTION_ID,
                MmsDatabase.MESSAGE_SIZE, MmsDatabase.EXPIRY, MmsDatabase.STATUS,
                MmsSmsColumns.DELIVERY_RECEIPT_COUNT, MmsSmsColumns.READ_RECEIPT_COUNT,
                MmsSmsColumns.MISMATCHED_IDENTITIES,
                MmsSmsColumns.SUBSCRIPTION_ID, MmsSmsColumns.EXPIRES_IN, MmsSmsColumns.EXPIRE_STARTED,
                MmsSmsColumns.NOTIFIED, MmsSmsColumns.READ,
                MmsDatabase.NETWORK_FAILURE, TRANSPORT, SmsDatabase.DURATION, SmsDatabase.COMMUNICATION_TYPE,
                SmsDatabase.PAYLOAD_TYPE,
                AttachmentDatabase.UNIQUE_ID,
                AttachmentDatabase.MMS_ID,
                AttachmentDatabase.SIZE,
                AttachmentDatabase.FILE_NAME,
                AttachmentDatabase.DATA,
                AttachmentDatabase.THUMBNAIL,
                AttachmentDatabase.CONTENT_TYPE,
                AttachmentDatabase.CONTENT_LOCATION,
                AttachmentDatabase.DIGEST,
                AttachmentDatabase.FAST_PREFLIGHT_ID,
                AttachmentDatabase.VOICE_NOTE,
                AttachmentDatabase.CONTENT_DISPOSITION,
                AttachmentDatabase.NAME,
                AttachmentDatabase.TRANSFER_STATE,
                AttachmentDatabase.DURATION,
                AttachmentDatabase.URL,
                AttachmentDatabase.THUMBNAIL_ASPECT_RATIO};
    }

    public static Set<String> getMmsColumnsPresent() {
        Set<String> mmsColumnsPresent = new HashSet<>();
        mmsColumnsPresent.add(MmsSmsColumns.ID);
        mmsColumnsPresent.add(MmsSmsColumns.READ);
        mmsColumnsPresent.add(MmsSmsColumns.THREAD_ID);
        mmsColumnsPresent.add(MmsSmsColumns.BODY);
        mmsColumnsPresent.add(MmsSmsColumns.ADDRESS);
        mmsColumnsPresent.add(MmsSmsColumns.ADDRESS_DEVICE_ID);
        mmsColumnsPresent.add(MmsSmsColumns.DELIVERY_RECEIPT_COUNT);
        mmsColumnsPresent.add(MmsSmsColumns.READ_RECEIPT_COUNT);
        mmsColumnsPresent.add(MmsSmsColumns.MISMATCHED_IDENTITIES);
        mmsColumnsPresent.add(MmsSmsColumns.SUBSCRIPTION_ID);
        mmsColumnsPresent.add(MmsSmsColumns.EXPIRES_IN);
        mmsColumnsPresent.add(MmsSmsColumns.EXPIRE_STARTED);
        mmsColumnsPresent.add(MmsDatabase.MESSAGE_TYPE);
        mmsColumnsPresent.add(MmsDatabase.MESSAGE_BOX);
        mmsColumnsPresent.add(MmsDatabase.DATE_SENT);
        mmsColumnsPresent.add(MmsDatabase.DATE_RECEIVED);
        mmsColumnsPresent.add(MmsDatabase.PART_COUNT);
        mmsColumnsPresent.add(MmsDatabase.CONTENT_LOCATION);
        mmsColumnsPresent.add(MmsDatabase.TRANSACTION_ID);
        mmsColumnsPresent.add(MmsDatabase.MESSAGE_SIZE);
        mmsColumnsPresent.add(MmsDatabase.EXPIRY);
        mmsColumnsPresent.add(MmsDatabase.NOTIFIED);
        mmsColumnsPresent.add(MmsDatabase.STATUS);
        mmsColumnsPresent.add(MmsDatabase.NETWORK_FAILURE);

        mmsColumnsPresent.add(AttachmentDatabase.ROW_ID);
        mmsColumnsPresent.add(AttachmentDatabase.UNIQUE_ID);
        mmsColumnsPresent.add(AttachmentDatabase.MMS_ID);
        mmsColumnsPresent.add(AttachmentDatabase.SIZE);
        mmsColumnsPresent.add(AttachmentDatabase.FILE_NAME);
        mmsColumnsPresent.add(AttachmentDatabase.DATA);
        mmsColumnsPresent.add(AttachmentDatabase.THUMBNAIL);
        mmsColumnsPresent.add(AttachmentDatabase.CONTENT_TYPE);
        mmsColumnsPresent.add(AttachmentDatabase.CONTENT_LOCATION);
        mmsColumnsPresent.add(AttachmentDatabase.DIGEST);
        mmsColumnsPresent.add(AttachmentDatabase.FAST_PREFLIGHT_ID);
        mmsColumnsPresent.add(AttachmentDatabase.VOICE_NOTE);
        mmsColumnsPresent.add(AttachmentDatabase.CONTENT_DISPOSITION);
        mmsColumnsPresent.add(AttachmentDatabase.NAME);
        mmsColumnsPresent.add(AttachmentDatabase.TRANSFER_STATE);
        mmsColumnsPresent.add(AttachmentDatabase.DURATION);
        mmsColumnsPresent.add(AttachmentDatabase.URL);

        return mmsColumnsPresent;
    }


    public static Set<String> getSmsColumnsPresent() {
        Set<String> smsColumnsPresent = new HashSet<>();
        smsColumnsPresent.add(MmsSmsColumns.ID);
        smsColumnsPresent.add(MmsSmsColumns.BODY);
        smsColumnsPresent.add(MmsSmsColumns.ADDRESS);
        smsColumnsPresent.add(MmsSmsColumns.ADDRESS_DEVICE_ID);
        smsColumnsPresent.add(MmsSmsColumns.READ);
        smsColumnsPresent.add(MmsSmsColumns.THREAD_ID);
        smsColumnsPresent.add(MmsSmsColumns.DELIVERY_RECEIPT_COUNT);
        smsColumnsPresent.add(MmsSmsColumns.READ_RECEIPT_COUNT);
        smsColumnsPresent.add(MmsSmsColumns.MISMATCHED_IDENTITIES);
        smsColumnsPresent.add(MmsSmsColumns.SUBSCRIPTION_ID);
        smsColumnsPresent.add(MmsSmsColumns.EXPIRES_IN);
        smsColumnsPresent.add(MmsSmsColumns.EXPIRE_STARTED);
        smsColumnsPresent.add(MmsSmsColumns.NOTIFIED);
        smsColumnsPresent.add(SmsDatabase.TYPE);
        smsColumnsPresent.add(SmsDatabase.SUBJECT);
        smsColumnsPresent.add(SmsDatabase.DATE_SENT);
        smsColumnsPresent.add(SmsDatabase.DATE_RECEIVED);
        smsColumnsPresent.add(SmsDatabase.STATUS);
        smsColumnsPresent.add(SmsDatabase.DURATION);
        smsColumnsPresent.add(SmsDatabase.COMMUNICATION_TYPE);
        smsColumnsPresent.add(SmsDatabase.PAYLOAD_TYPE);
        return smsColumnsPresent;
    }


    public MmsSmsDatabase(Context context, AccountContext accountContext, SQLiteOpenHelper databaseHelper) {
        super(context, accountContext, databaseHelper);
    }

    public Cursor getConversationAsc(long threadId) {
        String order = MmsSmsColumns.NORMALIZED_DATE_SENT + " ASC";
        String selection = MmsSmsColumns.THREAD_ID + " = " + threadId;
        Cursor cursor = queryTables(PROJECTION, selection, order, null);

        return cursor;
    }

    private Cursor queryTables(String[] projection, String selection, String order, String limit) {
        return queryTables(projection, selection, order, limit, null, null);
    }

    private Cursor queryTables(String[] projection, String selection, String order, String limit, String groupBy, String having) {
        String[] mmsProjection = getMmsProjection();

        String[] smsProjection = getSmsProjection();

        SQLiteQueryBuilder mmsQueryBuilder = new SQLiteQueryBuilder();
        SQLiteQueryBuilder smsQueryBuilder = new SQLiteQueryBuilder();

        mmsQueryBuilder.setDistinct(true);
        smsQueryBuilder.setDistinct(true);

        smsQueryBuilder.setTables(SmsDatabase.TABLE_NAME);
        mmsQueryBuilder.setTables(MmsDatabase.TABLE_NAME + " LEFT OUTER JOIN " +
                AttachmentDatabase.TABLE_NAME +
                " ON " + AttachmentDatabase.TABLE_NAME + "." + AttachmentDatabase.ROW_ID + " = " +
                " (SELECT " + AttachmentDatabase.TABLE_NAME + "." + AttachmentDatabase.ROW_ID +
                " FROM " + AttachmentDatabase.TABLE_NAME + " WHERE " +
                AttachmentDatabase.TABLE_NAME + "." + AttachmentDatabase.MMS_ID + " = " +
                MmsDatabase.TABLE_NAME + "." + MmsDatabase.ID + " LIMIT 1)");


        Set<String> mmsColumnsPresent = getMmsColumnsPresent();

        Set<String> smsColumnsPresent = getSmsColumnsPresent();

//        @SuppressWarnings("deprecation")
        String mmsSubQuery = mmsQueryBuilder.buildUnionSubQuery(TRANSPORT, mmsProjection, mmsColumnsPresent, 4, MMS_TRANSPORT, selection, null, null);
        ALog.d(TAG, "mmsSubQuery: " + mmsSubQuery + "\n");

//        @SuppressWarnings("deprecation")
        String smsSubQuery = smsQueryBuilder.buildUnionSubQuery(TRANSPORT, smsProjection, smsColumnsPresent, 4, SMS_TRANSPORT, selection, null, null);
        ALog.d(TAG, "smsSubQuery: " + smsSubQuery + "\n");

        SQLiteQueryBuilder unionQueryBuilder = new SQLiteQueryBuilder();
        String unionQuery = unionQueryBuilder.buildUnionQuery(new String[]{smsSubQuery, mmsSubQuery}, order, limit);
        ALog.d(TAG, "unionQuery: " + unionQuery + "\n");

        SQLiteQueryBuilder outerQueryBuilder = new SQLiteQueryBuilder();
        outerQueryBuilder.setTables("(" + unionQuery + ")");
//        @SuppressWarnings("deprecation")
        String query = outerQueryBuilder.buildQuery(projection, null, groupBy, having, null, null);
        ALog.d(TAG, "target query: " + query + "\n");

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        return db.rawQuery(query, null);
    }

    public Reader readerFor(@NonNull Cursor cursor, @Nullable MasterSecret masterSecret) {
        return new Reader(cursor, masterSecret);
    }

    public Reader readerFor(@NonNull Cursor cursor) {
        return new Reader(cursor);
    }

    public class Reader {

        private final Cursor cursor;
        private final Optional<MasterSecret> masterSecret;
        private EncryptingSmsDatabase.Reader smsReader;
        private MmsDatabase.Reader mmsReader;

        public Reader(Cursor cursor, @Nullable MasterSecret masterSecret) {
            this.cursor = cursor;
            this.masterSecret = Optional.fromNullable(masterSecret);
        }

        public Reader(Cursor cursor) {
            this(cursor, null);
        }

        private EncryptingSmsDatabase.Reader getSmsReader(EncryptingSmsDatabase encryptingSmsDatabase, SmsDatabase smsDatabase) {
            if (smsReader == null) {
                if (masterSecret.isPresent())
                    smsReader = encryptingSmsDatabase.readerFor(masterSecret.get(), cursor);
                else
                    smsReader = smsDatabase.readerFor(cursor);
            }

            return smsReader;
        }

        private MmsDatabase.Reader getMmsReader(MmsDatabase mmsDatabase) {
            if (mmsReader == null) {
                mmsReader = mmsDatabase.readerFor(masterSecret.orNull(), cursor);
            }

            return mmsReader;
        }

        public MessageRecord getNextMigrate(DatabaseFactory databaseFactory) {
            if (cursor == null || !cursor.moveToNext())
                return null;

            return getCurrentMigrate(databaseFactory);
        }

        public MessageRecord getCurrentMigrate(DatabaseFactory databaseFactory) {
            String type = cursor.getString(cursor.getColumnIndexOrThrow(TRANSPORT));

            if (MmsSmsDatabase.MMS_TRANSPORT.equals(type))
                return getMmsReader(databaseFactory.getMms()).getCurrentForMigrate(databaseFactory.getAttachments());
            else if (MmsSmsDatabase.SMS_TRANSPORT.equals(type))
                return getSmsReader(databaseFactory.getEncryptingSms(), databaseFactory.getSms()).getCurrentForMigrate();
            else
                throw new AssertionError("Bad type: " + type);
        }

        public void close() {
            cursor.close();
        }
    }
}
