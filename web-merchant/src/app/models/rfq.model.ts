export interface RfqItem {
  name: string;
  quantity: number;
  unit: string;
}

export interface SanitizedRfq {
  rfq_id: string;
  target_region: string;
  items_manifest: RfqItem[];
  delivery_preference: 'pickup_only' | 'delivery_required' | 'any';
  expires_at: string;
}

export interface SubmitBidRequest {
  merchant_id: string;
  total_price: number;
  fulfilment_type: 'pickup' | 'merchant_delivery';
  proforma_invoice_url: string;
  notes?: string;
}

export interface SupplierBid {
  id: string;
  rfq_id: string;
  merchant_id: string;
  total_price: number;
  fulfilment_type: 'pickup' | 'merchant_delivery';
  proforma_invoice_url: string;
  status: 'submitted' | 'accepted' | 'rejected' | 'expired';
  notes?: string;
  created_at: string;
}
