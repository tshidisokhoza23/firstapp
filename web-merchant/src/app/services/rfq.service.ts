import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SanitizedRfq, SubmitBidRequest, SupplierBid } from '../models/rfq.model';

@Injectable({
  providedIn: 'root'
})
export class RfqService {
  private baseUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  getOpenRfqs(): Observable<SanitizedRfq[]> {
    return this.http.get<SanitizedRfq[]>(`${this.baseUrl}/merchant/rfqs`);
  }

  submitBid(rfqId: string, bid: SubmitBidRequest): Observable<SupplierBid> {
    return this.http.post<SupplierBid>(`${this.baseUrl}/merchant/rfqs/${rfqId}/bids`, bid);
  }
}
