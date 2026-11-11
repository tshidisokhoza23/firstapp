import { Component, OnInit } from '@angular/core';
import { RfqService } from '../../services/rfq.service';
import { SanitizedRfq, SubmitBidRequest } from '../../models/rfq.model';

@Component({
  selector: 'app-rfq-list',
  templateUrl: './rfq-list.component.html',
  styleUrls: ['./rfq-list.component.scss']
})
export class RfqListComponent implements OnInit {
  rfqs: SanitizedRfq[] = [];
  selectedRfq: SanitizedRfq | null = null;
  loading = false;
  errorMessage = '';
  successMessage = '';

  merchantId = 'merchant-wholesaler-01';
  totalPrice = 0;
  fulfilmentType: 'pickup' | 'merchant_delivery' = 'merchant_delivery';
  proformaInvoiceUrl = '';
  notes = '';

  constructor(private rfqService: RfqService) {}

  ngOnInit(): void {
    this.fetchRfqs();
  }

  fetchRfqs(): void {
    this.loading = true;
    this.errorMessage = '';
    this.rfqService.getOpenRfqs().subscribe({
      next: (data) => {
        this.rfqs = data;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load tenders. Please try again.';
        this.loading = false;
      }
    });
  }

  openBidDialog(rfq: SanitizedRfq): void {
    this.selectedRfq = rfq;
    this.totalPrice = 0;
    this.proformaInvoiceUrl = '';
    this.notes = '';
    this.errorMessage = '';
    this.successMessage = '';
  }

  closeBidDialog(): void {
    this.selectedRfq = null;
  }

  submitBid(): void {
    if (!this.selectedRfq) return;
    if (this.totalPrice <= 0) {
      this.errorMessage = 'Please enter a valid total price.';
      return;
    }

    const payload: SubmitBidRequest = {
      merchant_id: this.merchantId,
      total_price: this.totalPrice,
      fulfilment_type: this.fulfilmentType,
      proforma_invoice_url: this.proformaInvoiceUrl || 'https://r2.kasistokvel.co.za/proforma/default.pdf',
      notes: this.notes
    };

    this.rfqService.submitBid(this.selectedRfq.rfq_id, payload).subscribe({
      next: (res) => {
        this.successMessage = `Bid submitted successfully for RFQ #${this.selectedRfq?.rfq_id}!`;
        this.closeBidDialog();
        this.fetchRfqs();
      },
      error: (err) => {
        this.errorMessage = err.error?.error || 'Failed to submit bid.';
      }
    });
  }
}
