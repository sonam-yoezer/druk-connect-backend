package com.drukconnect.drukconnect.util;

import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.entity.authentication.VouchWithdrawalRequest;
import com.drukconnect.drukconnect.repository.authentication.UserRepository;

import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VouchWithdrawalEmailSender {

    private final JavaMailSender mailSender;

    private final UserRepository userRepository;

    public VouchWithdrawalEmailSender(
            JavaMailSender mailSender,
            UserRepository userRepository
    ) {

        this.mailSender =
                mailSender;

        this.userRepository =
                userRepository;
    }


    public void notifyAdminsOfWithdrawalRequest(
            VouchWithdrawalRequest withdrawal
    ) {

        User buyer =
                withdrawal
                        .getVouch()
                        .getVoucherUser();

        User lister =
                withdrawal
                        .getVouch()
                        .getVouchedUser();


        List<User> admins =
                userRepository
                        .findActiveAdmins();


        for (
                User admin : admins
        ) {

            String html =
                    """
                    <div style="font-family:Arial,sans-serif;max-width:620px;margin:auto;">
                        <h2>New Vouch Withdrawal Request</h2>

                        <p>Hello %s,</p>

                        <p>
                            A Buyer has requested to withdraw an active vouch.
                        </p>

                        <p>
                            <strong>Buyer:</strong> %s<br/>
                            <strong>Buyer Email:</strong> %s
                        </p>

                        <p>
                            <strong>Lister:</strong> %s<br/>
                            <strong>Lister Email:</strong> %s
                        </p>

                        <p>
                            <strong>Reason:</strong><br/>
                            %s
                        </p>

                        <p>
                            Please review this request from the
                            DrukConnect admin panel.
                        </p>
                    </div>
                    """.formatted(

                            escapeHtml(
                                    fullName(
                                            admin
                                    )
                            ),

                            escapeHtml(
                                    fullName(
                                            buyer
                                    )
                            ),

                            escapeHtml(
                                    buyer.getEmail()
                            ),

                            escapeHtml(
                                    fullName(
                                            lister
                                    )
                            ),

                            escapeHtml(
                                    lister.getEmail()
                            ),

                            escapeHtml(
                                    withdrawal.getReason()
                            )
                    );

            send(
                    admin.getEmail(),
                    "New Vouch Withdrawal Request",
                    html
            );
        }
    }


    public void sendApprovedEmails(

            User buyer,

            User lister,

            VouchWithdrawalRequest withdrawal,

            long remainingActiveVouches

    ) {

        String buyerHtml =
                """
                <p>Hello %s,</p>

                <p>
                    Your request to withdraw your vouch for
                    <strong>%s</strong> has been approved.
                </p>
                """.formatted(
                        escapeHtml(
                                fullName(
                                        buyer
                                )
                        ),
                        escapeHtml(
                                fullName(
                                        lister
                                )
                        )
                );

        send(
                buyer.getEmail(),
                "Vouch withdrawal approved",
                buyerHtml
        );


        String listerHtml =
                """
                <p>Hello %s,</p>

                <p>
                    A vouch previously provided by
                    <strong>%s</strong> has been withdrawn
                    after admin approval.
                </p>

                <p>
                    You currently have
                    <strong>%d active vouch(es)</strong>.
                </p>

                %s
                """.formatted(

                        escapeHtml(
                                fullName(
                                        lister
                                )
                        ),

                        escapeHtml(
                                fullName(
                                        buyer
                                )
                        ),

                        remainingActiveVouches,

                        remainingActiveVouches < 2
                                ? """
                                  <p>
                                      Your account no longer meets the
                                      minimum 2-vouch requirement.
                                      When you sign in, DrukConnect will
                                      guide you through vouch recovery.
                                  </p>
                                  """
                                : ""
                );

        send(
                lister.getEmail(),
                "A vouch has been withdrawn",
                listerHtml
        );
    }


    public void sendRejectedEmail(

            User buyer,

            User lister,

            VouchWithdrawalRequest withdrawal

    ) {

        String html =
                """
                <p>Hello %s,</p>

                <p>
                    Your request to withdraw your vouch for
                    <strong>%s</strong> was rejected.
                </p>

                <p>
                    <strong>Reason:</strong><br/>
                    %s
                </p>

                <p>
                    Your vouch remains active.
                </p>
                """.formatted(

                        escapeHtml(
                                fullName(
                                        buyer
                                )
                        ),

                        escapeHtml(
                                fullName(
                                        lister
                                )
                        ),

                        escapeHtml(
                                withdrawal.getAdminReason()
                        )
                );

        send(
                buyer.getEmail(),
                "Vouch withdrawal request rejected",
                html
        );
    }


    private void send(

            String to,

            String subject,

            String html

    ) {

        try {

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8"
                    );

            helper.setTo(
                    to
            );

            helper.setSubject(
                    subject
            );

            helper.setText(
                    html,
                    true
            );

            mailSender.send(
                    message
            );

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Failed to send vouch withdrawal email",
                    ex
            );
        }
    }


    private String fullName(
            User user
    ) {

        return (
                user.getFirstName()
                        + " "
                        + user.getLastName()
        ).trim();
    }


    private String escapeHtml(
            String value
    ) {

        if (
                value == null
        ) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
