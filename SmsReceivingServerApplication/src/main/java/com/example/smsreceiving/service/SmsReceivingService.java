package com.example.smsreceiving.service;

import com.example.smsreceiving.model.EmailRecord;
import com.example.smsreceiving.repository.EmailRecordRepository;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

@Service
public class SmsReceivingService {

    private final EmailRecordRepository repository;

    public SmsReceivingService(EmailRecordRepository repository) {
        this.repository = repository;
    }

    // Fetch records from the database and process PENDING and ERROR records



public void processRecords() throws Exception {
    while (true) {
        // Fetch PENDING records
        List<EmailRecord> pendingRecords = repository.findByStatus("PENDING");
        if (!pendingRecords.isEmpty()) {
            sendToGateway(pendingRecords);
        }

        // Fetch ERROR records
        List<EmailRecord> errorRecords = repository.findByStatus("ERROR");
        if (!errorRecords.isEmpty()) {
            sendToGateway(errorRecords);
        }

        // Introduce a delay to avoid busy looping
        Thread.sleep(5000); // Delay in milliseconds (adjust as needed)
    }
}

    // Send records to the Gateway Server via socket
//    private void sendToGateway(List<EmailRecord> records) throws Exception {
//        try (Socket socket = new Socket("localhost", 8082)) {
//            OutputStream os = socket.getOutputStream();
//            for (EmailRecord record : records) {
//                String data = record.getId() + "," + record.getMobileNumber() + ","+ "," + record.getMessage() + "\n";
//                os.write(data.getBytes());
//                record.setStatus("PROCESSING");
//                repository.save(record);
//            }
//            os.flush();
//        }
//    }

    private void sendToGateway(List<EmailRecord> records) throws Exception {
        try (Socket socket = new Socket("localhost", 8082)) {
            OutputStream os = socket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            for (EmailRecord record : records) {
                String data = record.getId() + "," + record.getMobileNumber() + "," + record.getMessage() + "\n";
                os.write(data.getBytes());
                os.flush();

                // Read the reference number from the gateway server
                String referenceNumber = reader.readLine();
                System.out.println("Received Reference Number: " + referenceNumber);

                // Update the record with the reference number
                record.setReferenceNumber(referenceNumber);
                record.setStatus("PROCESSING");
                repository.save(record);
            }
        }
    }



    // Update record status based on Gateway response
    public void updateRecordStatus(Long id, String status) {
        EmailRecord record = repository.findById(id).orElseThrow(() -> new RuntimeException("Record not found"));
        record.setStatus(status);
        repository.save(record);
    }
}
