package com.alertyai.app.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class OrgRepository_Factory implements Factory<OrgRepository> {
  @Override
  public OrgRepository get() {
    return newInstance();
  }

  public static OrgRepository_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static OrgRepository newInstance() {
    return new OrgRepository();
  }

  private static final class InstanceHolder {
    private static final OrgRepository_Factory INSTANCE = new OrgRepository_Factory();
  }
}
