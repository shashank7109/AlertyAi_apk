package com.alertyai.app.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.alertyai.app.data.model.Reminder;
import com.alertyai.app.data.model.RepeatInterval;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ReminderDao_Impl implements ReminderDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Reminder> __insertionAdapterOfReminder;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<Reminder> __deletionAdapterOfReminder;

  private final EntityDeletionOrUpdateAdapter<Reminder> __updateAdapterOfReminder;

  private final SharedSQLiteStatement __preparedStmtOfMarkDone;

  public ReminderDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfReminder = new EntityInsertionAdapter<Reminder>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `reminders` (`id`,`title`,`body`,`triggerAt`,`isRepeating`,`repeatInterval`,`isDone`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Reminder entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getBody());
        statement.bindLong(4, entity.getTriggerAt());
        final int _tmp = entity.isRepeating() ? 1 : 0;
        statement.bindLong(5, _tmp);
        final String _tmp_1 = __converters.fromRepeat(entity.getRepeatInterval());
        statement.bindString(6, _tmp_1);
        final int _tmp_2 = entity.isDone() ? 1 : 0;
        statement.bindLong(7, _tmp_2);
        statement.bindLong(8, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfReminder = new EntityDeletionOrUpdateAdapter<Reminder>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `reminders` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Reminder entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfReminder = new EntityDeletionOrUpdateAdapter<Reminder>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `reminders` SET `id` = ?,`title` = ?,`body` = ?,`triggerAt` = ?,`isRepeating` = ?,`repeatInterval` = ?,`isDone` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Reminder entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getBody());
        statement.bindLong(4, entity.getTriggerAt());
        final int _tmp = entity.isRepeating() ? 1 : 0;
        statement.bindLong(5, _tmp);
        final String _tmp_1 = __converters.fromRepeat(entity.getRepeatInterval());
        statement.bindString(6, _tmp_1);
        final int _tmp_2 = entity.isDone() ? 1 : 0;
        statement.bindLong(7, _tmp_2);
        statement.bindLong(8, entity.getCreatedAt());
        statement.bindLong(9, entity.getId());
      }
    };
    this.__preparedStmtOfMarkDone = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE reminders SET isDone = 1 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertReminder(final Reminder reminder,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfReminder.insertAndReturnId(reminder);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteReminder(final Reminder reminder,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfReminder.handle(reminder);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateReminder(final Reminder reminder,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfReminder.handle(reminder);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markDone(final int id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkDone.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkDone.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Reminder>> getAllReminders() {
    final String _sql = "SELECT * FROM reminders ORDER BY isDone ASC, triggerAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"reminders"}, new Callable<List<Reminder>>() {
      @Override
      @NonNull
      public List<Reminder> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfBody = CursorUtil.getColumnIndexOrThrow(_cursor, "body");
          final int _cursorIndexOfTriggerAt = CursorUtil.getColumnIndexOrThrow(_cursor, "triggerAt");
          final int _cursorIndexOfIsRepeating = CursorUtil.getColumnIndexOrThrow(_cursor, "isRepeating");
          final int _cursorIndexOfRepeatInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatInterval");
          final int _cursorIndexOfIsDone = CursorUtil.getColumnIndexOrThrow(_cursor, "isDone");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<Reminder> _result = new ArrayList<Reminder>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Reminder _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpBody;
            _tmpBody = _cursor.getString(_cursorIndexOfBody);
            final long _tmpTriggerAt;
            _tmpTriggerAt = _cursor.getLong(_cursorIndexOfTriggerAt);
            final boolean _tmpIsRepeating;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRepeating);
            _tmpIsRepeating = _tmp != 0;
            final RepeatInterval _tmpRepeatInterval;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfRepeatInterval);
            _tmpRepeatInterval = __converters.toRepeat(_tmp_1);
            final boolean _tmpIsDone;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsDone);
            _tmpIsDone = _tmp_2 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new Reminder(_tmpId,_tmpTitle,_tmpBody,_tmpTriggerAt,_tmpIsRepeating,_tmpRepeatInterval,_tmpIsDone,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<Reminder>> getUpcomingReminders(final long now) {
    final String _sql = "SELECT * FROM reminders WHERE isDone = 0 AND triggerAt >= ? ORDER BY triggerAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, now);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"reminders"}, new Callable<List<Reminder>>() {
      @Override
      @NonNull
      public List<Reminder> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfBody = CursorUtil.getColumnIndexOrThrow(_cursor, "body");
          final int _cursorIndexOfTriggerAt = CursorUtil.getColumnIndexOrThrow(_cursor, "triggerAt");
          final int _cursorIndexOfIsRepeating = CursorUtil.getColumnIndexOrThrow(_cursor, "isRepeating");
          final int _cursorIndexOfRepeatInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatInterval");
          final int _cursorIndexOfIsDone = CursorUtil.getColumnIndexOrThrow(_cursor, "isDone");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<Reminder> _result = new ArrayList<Reminder>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Reminder _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpBody;
            _tmpBody = _cursor.getString(_cursorIndexOfBody);
            final long _tmpTriggerAt;
            _tmpTriggerAt = _cursor.getLong(_cursorIndexOfTriggerAt);
            final boolean _tmpIsRepeating;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRepeating);
            _tmpIsRepeating = _tmp != 0;
            final RepeatInterval _tmpRepeatInterval;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfRepeatInterval);
            _tmpRepeatInterval = __converters.toRepeat(_tmp_1);
            final boolean _tmpIsDone;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsDone);
            _tmpIsDone = _tmp_2 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new Reminder(_tmpId,_tmpTitle,_tmpBody,_tmpTriggerAt,_tmpIsRepeating,_tmpRepeatInterval,_tmpIsDone,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getReminderById(final int id, final Continuation<? super Reminder> $completion) {
    final String _sql = "SELECT * FROM reminders WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Reminder>() {
      @Override
      @Nullable
      public Reminder call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfBody = CursorUtil.getColumnIndexOrThrow(_cursor, "body");
          final int _cursorIndexOfTriggerAt = CursorUtil.getColumnIndexOrThrow(_cursor, "triggerAt");
          final int _cursorIndexOfIsRepeating = CursorUtil.getColumnIndexOrThrow(_cursor, "isRepeating");
          final int _cursorIndexOfRepeatInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatInterval");
          final int _cursorIndexOfIsDone = CursorUtil.getColumnIndexOrThrow(_cursor, "isDone");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final Reminder _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpBody;
            _tmpBody = _cursor.getString(_cursorIndexOfBody);
            final long _tmpTriggerAt;
            _tmpTriggerAt = _cursor.getLong(_cursorIndexOfTriggerAt);
            final boolean _tmpIsRepeating;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRepeating);
            _tmpIsRepeating = _tmp != 0;
            final RepeatInterval _tmpRepeatInterval;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfRepeatInterval);
            _tmpRepeatInterval = __converters.toRepeat(_tmp_1);
            final boolean _tmpIsDone;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsDone);
            _tmpIsDone = _tmp_2 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new Reminder(_tmpId,_tmpTitle,_tmpBody,_tmpTriggerAt,_tmpIsRepeating,_tmpRepeatInterval,_tmpIsDone,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
