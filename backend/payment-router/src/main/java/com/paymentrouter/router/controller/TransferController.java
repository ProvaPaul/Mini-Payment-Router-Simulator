package com.paymentrouter.router.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.paymentrouter.router.dto.TransferRequest;
import com.paymentrouter.router.dto.TransferResponse;
import com.paymentrouter.router.service.TransferService;

/**
 * HTTP entry point for transfers. Delegates all work to {@link TransferService}.
 */
@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /** POST /api/transfers: executes a transfer and returns the recorded transaction. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse executeTransfer(@RequestBody TransferRequest request) {
        return transferService.executeTransfer(request);
    }
}
