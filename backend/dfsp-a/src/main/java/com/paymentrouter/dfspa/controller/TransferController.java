package com.paymentrouter.dfspa.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentrouter.dfspa.dto.TransferRequest;
import com.paymentrouter.dfspa.dto.TransferResponse;
import com.paymentrouter.dfspa.service.TransferService;

/**
 * DFSP-A's only API, called by the Payment Router.
 * <p>
 * A processed transfer always returns HTTP 200; the decision (SUCCESS or FAILED) is in the body.
 */
@RestController
@RequestMapping("/api/dfsp-a/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public TransferResponse receiveTransfer(@RequestBody TransferRequest request) {
        return transferService.process(request);
    }
}
