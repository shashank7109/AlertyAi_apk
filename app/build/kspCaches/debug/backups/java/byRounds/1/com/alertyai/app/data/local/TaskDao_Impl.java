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
import com.alertyai.app.data.model.Priority;
import com.alertyai.app.data.model.Task;
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
public final class TaskDao_Impl implements TaskDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Task> __insertionAdapterOfTask;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<Task> __deletionAdapterOfTask;

  private final EntityDeletionOrUpdateAdapter<Task> __updateAdapterOfTask;

  private final SharedSQLiteStatement __preparedStmtOfSetTaskDone;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllCompleted;

  public TaskDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTask = new EntityInsertionAdapter<Task>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `tasks` (`id`,`title`,`note`,`priority`,`isDone`,`dueDate`,`dueTime`,`alarmEnabled`,`remindMinsBefore`,`location`,`subtasksJson`,`checklistJson`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Task entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getNote());
        final String _tmp = __converters.fromPriority(entity.getPriority());
        statement.bindString(4, _tmp);
        final int _tmp_1 = entity.isDone() ? 1 : 0;
        statement.bindLong(5, _tmp_1);
        if (entity.getDueDate() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getDueDate());
        }
        if (entity.getDueTime() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getDueTime());
        }
        final int _tmp_2 = entity.getAlarmEnabled() ? 1 : 0;
        statement.bindLong(8, _tmp_2);
        statement.bindLong(9, entity.getRemindMinsBefore());
        statement.bindString(10, entity.getLocation());
        statement.bindString(11, entity.getSubtasksJson());
        statement.bindString(12, entity.getChecklistJson());
        statement.bindLong(13, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfTask = new EntityDeletionOrUpdateAdapter<Task>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `tasks` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Task entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfTask = new EntityDeletionOrUpdateAdapter<Task>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `tasks` SET `id` = ?,`title` = ?,`note` = ?,`priority` = ?,`isDone` = ?,`dueDate` = ?,`dueTime` = ?,`alarmEnabled` = ?,`remindMinsBefore` = ?,`location` = ?,`subtasksJson` = ?,`checklistJson` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Task entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getNote());
        final String _tmp = __converters.fromPriority(entity.getPriority());
        statement.bindString(4, _tmp);
        final int _tmp_1 = entity.isDone() ? 1 : 0;
        statement.bindLong(5, _tmp_1);
        if (entity.getDueDate() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getDueDate());
        }
        if (entity.getDueTime() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getDueTime());
        }
        final int _tmp_2 = entity.getAlarmEnabled() ? 1 : 0;
        statement.bindLong(8, _tmp_2);
        statement.bindLong(9, entity.getRemindMinsBefore());
        statement.bindString(10, entity.getLocation());
        statement.bindString(11, entity.getSubtasksJson());
        statement.bindString(12, entity.getChecklistJson());
        statement.bindLong(13, entity.getCreatedAt());
        statement.bindLong(14, entity.getId());
      }
    };
    this.__preparedStmtOfSetTaskDone = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE tasks SET isDone = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllCompleted = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM tasks WHERE isDone = 1";
        return _query;
      }
    };
  }

  @Override
  public Object insertTask(final Task task, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfTask.insertAndReturnId(task);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteTask(final Task task, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfTask.handle(task);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTask(final Task task, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfTask.handle(task);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object setTaskDone(final int id, final boolean done,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetTaskDone.acquire();
        int _argIndex = 1;
        final int _tmp = done ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
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
          __preparedStmtOfSetTaskDone.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllCompleted(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllCompleted.acquire();
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
          __preparedStmtOfDeleteAllCompleted.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Task>> getAllTasks() {
    final String _sql = "SELECT * FROM tasks ORDER BY isDone ASC, CASE priority WHEN 'HIGH' THEN 0 WHEN 'NORMAL' THEN 1 ELSE 2 END, createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tasks"}, new Callable<List<Task>>() {
      @Override
      @NonNull
      public List<Task> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfIsDone = CursorUtil.getColumnIndexOrThrow(_cursor, "isDone");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
          final int _cursorIndexOfDueTime = CursorUtil.getColumnIndexOrThrow(_cursor, "dueTime");
          final int _cursorIndexOfAlarmEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "alarmEnabled");
          final int _cursorIndexOfRemindMinsBefore = CursorUtil.getColumnIndexOrThrow(_cursor, "remindMinsBefore");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfSubtasksJson = CursorUtil.getColumnIndexOrThrow(_cursor, "subtasksJson");
          final int _cursorIndexOfChecklistJson = CursorUtil.getColumnIndexOrThrow(_cursor, "checklistJson");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<Task> _result = new ArrayList<Task>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Task _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final Priority _tmpPriority;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfPriority);
            _tmpPriority = __converters.toPriority(_tmp);
            final boolean _tmpIsDone;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDone);
            _tmpIsDone = _tmp_1 != 0;
            final Long _tmpDueDate;
            if (_cursor.isNull(_cursorIndexOfDueDate)) {
              _tmpDueDate = null;
            } else {
              _tmpDueDate = _cursor.getLong(_cursorIndexOfDueDate);
            }
            final Long _tmpDueTime;
            if (_cursor.isNull(_cursorIndexOfDueTime)) {
              _tmpDueTime = null;
            } else {
              _tmpDueTime = _cursor.getLong(_cursorIndexOfDueTime);
            }
            final boolean _tmpAlarmEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfAlarmEnabled);
            _tmpAlarmEnabled = _tmp_2 != 0;
            final int _tmpRemindMinsBefore;
            _tmpRemindMinsBefore = _cursor.getInt(_cursorIndexOfRemindMinsBefore);
            final String _tmpLocation;
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            final String _tmpSubtasksJson;
            _tmpSubtasksJson = _cursor.getString(_cursorIndexOfSubtasksJson);
            final String _tmpChecklistJson;
            _tmpChecklistJson = _cursor.getString(_cursorIndexOfChecklistJson);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new Task(_tmpId,_tmpTitle,_tmpNote,_tmpPriority,_tmpIsDone,_tmpDueDate,_tmpDueTime,_tmpAlarmEnabled,_tmpRemindMinsBefore,_tmpLocation,_tmpSubtasksJson,_tmpChecklistJson,_tmpCreatedAt);
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
  public Flow<List<Task>> getTodayTasks() {
    final String _sql = "SELECT * FROM tasks WHERE isDone = 0 ORDER BY dueDate ASC, createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tasks"}, new Callable<List<Task>>() {
      @Override
      @NonNull
      public List<Task> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfIsDone = CursorUtil.getColumnIndexOrThrow(_cursor, "isDone");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
          final int _cursorIndexOfDueTime = CursorUtil.getColumnIndexOrThrow(_cursor, "dueTime");
          final int _cursorIndexOfAlarmEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "alarmEnabled");
          final int _cursorIndexOfRemindMinsBefore = CursorUtil.getColumnIndexOrThrow(_cursor, "remindMinsBefore");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfSubtasksJson = CursorUtil.getColumnIndexOrThrow(_cursor, "subtasksJson");
          final int _cursorIndexOfChecklistJson = CursorUtil.getColumnIndexOrThrow(_cursor, "checklistJson");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<Task> _result = new ArrayList<Task>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Task _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final Priority _tmpPriority;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfPriority);
            _tmpPriority = __converters.toPriority(_tmp);
            final boolean _tmpIsDone;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDone);
            _tmpIsDone = _tmp_1 != 0;
            final Long _tmpDueDate;
            if (_cursor.isNull(_cursorIndexOfDueDate)) {
              _tmpDueDate = null;
            } else {
              _tmpDueDate = _cursor.getLong(_cursorIndexOfDueDate);
            }
            final Long _tmpDueTime;
            if (_cursor.isNull(_cursorIndexOfDueTime)) {
              _tmpDueTime = null;
            } else {
              _tmpDueTime = _cursor.getLong(_cursorIndexOfDueTime);
            }
            final boolean _tmpAlarmEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfAlarmEnabled);
            _tmpAlarmEnabled = _tmp_2 != 0;
            final int _tmpRemindMinsBefore;
            _tmpRemindMinsBefore = _cursor.getInt(_cursorIndexOfRemindMinsBefore);
            final String _tmpLocation;
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            final String _tmpSubtasksJson;
            _tmpSubtasksJson = _cursor.getString(_cursorIndexOfSubtasksJson);
            final String _tmpChecklistJson;
            _tmpChecklistJson = _cursor.getString(_cursorIndexOfChecklistJson);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new Task(_tmpId,_tmpTitle,_tmpNote,_tmpPriority,_tmpIsDone,_tmpDueDate,_tmpDueTime,_tmpAlarmEnabled,_tmpRemindMinsBefore,_tmpLocation,_tmpSubtasksJson,_tmpChecklistJson,_tmpCreatedAt);
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
  public Object getTaskById(final int id, final Continuation<? super Task> $completion) {
    final String _sql = "SELECT * FROM tasks WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Task>() {
      @Override
      @Nullable
      public Task call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfIsDone = CursorUtil.getColumnIndexOrThrow(_cursor, "isDone");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
          final int _cursorIndexOfDueTime = CursorUtil.getColumnIndexOrThrow(_cursor, "dueTime");
          final int _cursorIndexOfAlarmEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "alarmEnabled");
          final int _cursorIndexOfRemindMinsBefore = CursorUtil.getColumnIndexOrThrow(_cursor, "remindMinsBefore");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfSubtasksJson = CursorUtil.getColumnIndexOrThrow(_cursor, "subtasksJson");
          final int _cursorIndexOfChecklistJson = CursorUtil.getColumnIndexOrThrow(_cursor, "checklistJson");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final Task _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final Priority _tmpPriority;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfPriority);
            _tmpPriority = __converters.toPriority(_tmp);
            final boolean _tmpIsDone;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDone);
            _tmpIsDone = _tmp_1 != 0;
            final Long _tmpDueDate;
            if (_cursor.isNull(_cursorIndexOfDueDate)) {
              _tmpDueDate = null;
            } else {
              _tmpDueDate = _cursor.getLong(_cursorIndexOfDueDate);
            }
            final Long _tmpDueTime;
            if (_cursor.isNull(_cursorIndexOfDueTime)) {
              _tmpDueTime = null;
            } else {
              _tmpDueTime = _cursor.getLong(_cursorIndexOfDueTime);
            }
            final boolean _tmpAlarmEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfAlarmEnabled);
            _tmpAlarmEnabled = _tmp_2 != 0;
            final int _tmpRemindMinsBefore;
            _tmpRemindMinsBefore = _cursor.getInt(_cursorIndexOfRemindMinsBefore);
            final String _tmpLocation;
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            final String _tmpSubtasksJson;
            _tmpSubtasksJson = _cursor.getString(_cursorIndexOfSubtasksJson);
            final String _tmpChecklistJson;
            _tmpChecklistJson = _cursor.getString(_cursorIndexOfChecklistJson);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new Task(_tmpId,_tmpTitle,_tmpNote,_tmpPriority,_tmpIsDone,_tmpDueDate,_tmpDueTime,_tmpAlarmEnabled,_tmpRemindMinsBefore,_tmpLocation,_tmpSubtasksJson,_tmpChecklistJson,_tmpCreatedAt);
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

  @Override
  public Object getAllTitles(final Continuation<? super List<String>> $completion) {
    final String _sql = "SELECT title FROM tasks";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
            _result.add(_item);
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
