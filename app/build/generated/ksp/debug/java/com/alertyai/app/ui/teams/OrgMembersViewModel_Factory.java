package com.alertyai.app.ui.teams;

import com.alertyai.app.data.repository.OrgRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class OrgMembersViewModel_Factory implements Factory<OrgMembersViewModel> {
  private final Provider<OrgRepository> repositoryProvider;

  public OrgMembersViewModel_Factory(Provider<OrgRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public OrgMembersViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static OrgMembersViewModel_Factory create(Provider<OrgRepository> repositoryProvider) {
    return new OrgMembersViewModel_Factory(repositoryProvider);
  }

  public static OrgMembersViewModel newInstance(OrgRepository repository) {
    return new OrgMembersViewModel(repository);
  }
}
