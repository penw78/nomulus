// Copyright 2017 The Nomulus Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package google.registry.model.transfer;

import static google.registry.util.CollectionUtils.nullToEmptyImmutableCopy;

import com.google.common.collect.ImmutableSet;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.IgnoreSave;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.condition.IfNull;
import google.registry.model.Buildable;
import google.registry.model.EppResource;
import google.registry.model.billing.BillingEvent;
import google.registry.model.domain.Period;
import google.registry.model.domain.Period.Unit;
import google.registry.model.eppcommon.Trid;
import google.registry.model.poll.PollMessage;
import java.util.Set;
import javax.annotation.Nullable;
import org.joda.time.DateTime;

/**
 * Common transfer data for {@link EppResource} types. Only applies to domains and contacts;
 * hosts are implicitly transferred with their superordinate domain.
 */
@Embed
@Unindex
public class TransferData extends BaseTransferObject implements Buildable {

  public static final TransferData EMPTY = new TransferData();

  /** The transaction id of the most recent transfer request (or null if there never was one). */
  Trid transferRequestTrid;

  /**
   * The period to extend the registration upon completion of the transfer.
   *
   * <p>By default, domain transfers are for one year. This can be changed to zero by using the
   * superuser EPP extension.
   */
  Period transferPeriod = Period.create(1, Unit.YEARS);

  /**
   * The registration expiration time resulting from the approval - speculative or actual - of the
   * most recent transfer request, applicable for domains only.
   *
   * <p>For pending transfers, this is the expiration time that will take effect under a projected
   * server approval. For approved transfers, this is the actual expiration time of the domain as
   * of the moment of transfer completion. For rejected or cancelled transfers, this field will be
   * reset to null.
   *
   * <p>Note that even when this field is set, it does not necessarily mean that the post-transfer
   * domain has a new expiration time.  Superuser transfers may not include a bundled 1 year renewal
   * at all, or even when a renewal is bundled, for a transfer during the autorenew grace period the
   * bundled renewal simply subsumes the recent autorenewal, resulting in the same expiration time.
   */
  // TODO(b/36405140): backfill this field for existing domains to which it should apply.
  DateTime transferredRegistrationExpirationTime;

  /**
   * The billing event and poll messages associated with a server-approved transfer.
   *
   * <p>This field should be null if there is not currently a pending transfer or if the object
   * being transferred is not a domain. If there is a pending transfer for a domain there should be
   * a number of poll messages and billing events for both the gaining and losing registrars. If the
   * pending transfer is explicitly approved, rejected or cancelled, the referenced entities should
   * be deleted.
   */
  @IgnoreSave(IfNull.class)
  Set<Key<? extends TransferServerApproveEntity>> serverApproveEntities;

  /**
   * The regular one-time billing event that will be charged for a server-approved transfer.
   *
   * <p>This field should be null if there is not currently a pending transfer or if the object
   * being transferred is not a domain.
   */
  @IgnoreSave(IfNull.class)
  Key<BillingEvent.OneTime> serverApproveBillingEvent;

  /**
   * The autorenew billing event that should be associated with this resource after the transfer.
   *
   * <p>This field should be null if there is not currently a pending transfer or if the object
   * being transferred is not a domain.
   */
  @IgnoreSave(IfNull.class)
  Key<BillingEvent.Recurring> serverApproveAutorenewEvent;

  /**
   * The autorenew poll message that should be associated with this resource after the transfer.
   *
   * <p>This field should be null if there is not currently a pending transfer or if the object
   * being transferred is not a domain.
   */
  @IgnoreSave(IfNull.class)
  Key<PollMessage.Autorenew> serverApproveAutorenewPollMessage;

  @Nullable
  public Trid getTransferRequestTrid() {
    return transferRequestTrid;
  }

  public Period getTransferPeriod() {
    return transferPeriod;
  }

  @Nullable
  public DateTime getTransferredRegistrationExpirationTime() {
    return transferredRegistrationExpirationTime;
  }

  public ImmutableSet<Key<? extends TransferServerApproveEntity>> getServerApproveEntities() {
    return nullToEmptyImmutableCopy(serverApproveEntities);
  }

  @Nullable
  public Key<BillingEvent.OneTime> getServerApproveBillingEvent() {
    return serverApproveBillingEvent;
  }

  @Nullable
  public Key<BillingEvent.Recurring> getServerApproveAutorenewEvent() {
    return serverApproveAutorenewEvent;
  }

  @Nullable
  public Key<PollMessage.Autorenew> getServerApproveAutorenewPollMessage() {
    return serverApproveAutorenewPollMessage;
  }

  @Override
  public Builder asBuilder() {
    return new Builder(clone(this));
  }

  /**
   * Returns a fresh Builder populated only with the constant fields of this TransferData, i.e.
   * those that are fixed and unchanging throughout the transfer process.
   *
   * <p>These fields are:
   * <ul>
   *   <li>transferRequestTrid
   *   <li>transferRequestTime
   *   <li>gainingClientId
   *   <li>losingClientId
   *   <li>transferPeriod
   * </ul>
   */
  public Builder copyConstantFieldsToBuilder() {
    return new Builder()
        .setTransferRequestTrid(this.transferRequestTrid)
        .setTransferRequestTime(this.transferRequestTime)
        .setGainingClientId(this.gainingClientId)
        .setLosingClientId(this.losingClientId)
        .setTransferPeriod(this.transferPeriod);
  }

  /** Builder for {@link TransferData} because it is immutable. */
  public static class Builder extends BaseTransferObject.Builder<TransferData, Builder> {

    /** Create a {@link Builder} wrapping a new instance. */
    public Builder() {}

    /** Create a {@link Builder} wrapping the given instance. */
    private Builder(TransferData instance) {
      super(instance);
    }

    public Builder setTransferRequestTrid(Trid transferRequestTrid) {
      getInstance().transferRequestTrid = transferRequestTrid;
      return this;
    }

    public Builder setTransferPeriod(Period transferPeriod) {
      getInstance().transferPeriod = transferPeriod;
      return this;
    }

    public Builder setTransferredRegistrationExpirationTime(
        DateTime transferredRegistrationExpirationTime) {
      getInstance().transferredRegistrationExpirationTime = transferredRegistrationExpirationTime;
      return this;
    }

    public Builder setServerApproveEntities(
        ImmutableSet<Key<? extends TransferServerApproveEntity>> serverApproveEntities) {
      getInstance().serverApproveEntities = serverApproveEntities;
      return this;
    }

    public Builder setServerApproveBillingEvent(
        Key<BillingEvent.OneTime> serverApproveBillingEvent) {
      getInstance().serverApproveBillingEvent = serverApproveBillingEvent;
      return this;
    }

    public Builder setServerApproveAutorenewEvent(
        Key<BillingEvent.Recurring> serverApproveAutorenewEvent) {
      getInstance().serverApproveAutorenewEvent = serverApproveAutorenewEvent;
      return this;
    }

    public Builder setServerApproveAutorenewPollMessage(
        Key<PollMessage.Autorenew> serverApproveAutorenewPollMessage) {
      getInstance().serverApproveAutorenewPollMessage = serverApproveAutorenewPollMessage;
      return this;
    }
  }

  /**
   * Marker interface for objects that are written in anticipation of a server approval, and
   * therefore need to be deleted under any other outcome.
   */
  public interface TransferServerApproveEntity {}
}
